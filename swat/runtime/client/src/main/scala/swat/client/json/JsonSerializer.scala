package swat.client.json

import scala.concurrent._
import ExecutionContext.Implicits.global
import swat.client.swat
import _root_.swat.js.CommonScope._
import _root_.swat.common._
import _root_.swat.js

/**
 * The client side JSON (de)serializer. Currently the core logic is implemented natively as it is much more convenient
 * but could be in the future reqritten to Scala in order to share some of its functionality with the server side
 * (de)serializer.
 */
object JsonSerializer {

    /** Serializes the specified value to Swat JSON. */
    def serialize(value: Any): String = js.native {"""
        var visitedObjects = [];
        var serializedObjects = [];
        var id = 0;

        var findVisitedObject = function(obj) {
            for (var i in visitedObjects) {
                if (visitedObjects[i].obj === obj) {
                    return visitedObjects[i];
                }
            }
            return null;
        };

        var serializeValue = function(value) {
            if (swat.isInstanceOf(value, scala.Array)) {
                return serializeArray(value);
            } if (swat.isSwatObject(value)) {
                return serializeAnyRef(value);
            }
            return value;
        };

        var serializeArray = function(array) {
            var jsArray = array.jsArray();
            var result = [];
            for (var i in jsArray) {
                result.push(serializeValue(jsArray[i]));
            }
            return result;
        };

        var serializeAnyRef = function(obj) {
            var reference = obj.$class.typeIdentifier;
            if (!obj.$class.isSingleton) {
                var visitedObject = findVisitedObject(obj);
                if (visitedObject != null) {
                    reference = visitedObject.id;
                } else {
                    visitedObjects.push({
                        id: id,
                        obj: obj
                    });
                    reference = id;
                    id++;
                    serializeObject(obj, reference);
                }
            }
            return { $ref: reference };
        };

        var serializeObject = function(obj, id) {
            var serializedObject = {
                $id: id,
                $type: obj.$class.typeIdentifier
            };
            for (var field in obj.$fields) {
                serializedObject[field] = serializeValue(obj.$fields[field]);
            }
            serializedObjects.push(serializedObject);
        };

        return JSON.stringify({
            $value: serializeValue(value),
            $objects: serializedObjects
        });
    """}

    /** Deserializes the specified value from Swat JSON. */
    def deserialize(json: String): Future[Any] = {
        val obj = JSON.parse(json)
        val loadedTypes = swat.jsArrayToScalaArray(swat.loadedTypes)
        val missingTypes = findMissingTypes(obj)

        if (missingTypes.length > 0) {
            // Not all the types present in the result are loaded. Therefore they have to be loaded, the code with the
            // types executed and the deserialization performed after that.
            TypeLoader.get(missingTypes, loadedTypes).map { code =>
                eval(code)
                deserialize(obj)
            }
        } else {
            // All the necessary types are loaded so deserialization can be done immediately.
            Future(deserialize(obj))
        }
    }

    /* Finds all types in the specified Swat JSON that aren't loaded yet. */
    def findMissingTypes(swatJson: Any): Array[String] = js.native {"""
        var missingTypes = [];

        var processType = function(typeIdentifier) {
            if (!swat.isLoaded(typeIdentifier)) {
                missingTypes.push(typeIdentifier);
            }
        }

        for (var i in swatJson.$objects) {
            var obj = swatJson.$objects[i];
            processType(obj.$type);
            for (var j in obj.$fields) {
                var field = obj.$fields[j];
                if (swat.isJsObject(field) && swat.isJsString(field.$ref)) {
                    processType(field.$ref);
                }
            }
        }

        return swat.jsArrayToScalaArray(missingTypes);
    """}

    /* Deserializes the specified Swat JSON and returns the root value. */
    def deserialize(swatJson: Any): Any = js.native {"""
        var deserializedObjects = {};

        var deserializeValue = function(value) {
            if (swat.isJsArray(value)) {
                var result = [];
                for (var i in value) {
                    result.push(deserializeValue(value[i]));
                }
                return swat.jsArrayToScalaArray(result);
            } else if (swat.isJsObject(value)) {
                if (swat.isJsString(value.$ref)) {
                    return swat.access(value.$ref)();
                } else {
                    return deserializedObjects[value.$ref];
                }
            } else {
                return value;
            }
        }

        // Instantiate all the objects.
        for (var i in swatJson.$objects) {
            var obj = swatJson.$objects[i];

            // A dummy constructor that just invokes the constructor common for all objects and setups the prototypes
            // properly. The standard constructor can't be used because parameters of it can't be currently inspected
            // during runtime, therefore passed in, so it can't be ensured it wouldn't throw any exceptions.
            var prototype = swat.access(obj.$type).prototype;
            var constructor = function() {
                this.$prototype = prototype;
                scala.Any.$init$.call(this);
            };
            constructor.prototype = prototype;

            // Invoke the dummy constructor and register the object to the deserialized objects.
            deserializedObjects[obj.$id] = new constructor();
        }

        // Deserialize values of all fields of the objects.
        for (var i in swatJson.$objects) {
            var obj = swatJson.$objects[i];
            var target = deserializedObjects[obj.$id];
            for (var j in obj) {
                if (j !== '$id' && j !== '$type') {
                    target.$fields[j] = deserializeValue(obj[j]);
                }
            }
        }

        // Return the deserialized root value.
        return deserializeValue(swatJson.$value);
    """}
}
