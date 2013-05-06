package swat.common.json

import play.api.libs.json._
import scala.reflect.runtime.universe._
import scala.collection.mutable
import scala.runtime.BoxedUnit

/**
 * A custom JSON object serializer and deserializer. The JSON always has the same structure no matter what actually
 * the serialized object is. The root JSON node is an object with fields "value" which contains the object that is
 * being serialized and "objects" which contains all instances in the serialization object graph. An instance has
 * it's id (field "$id") and type (field "$type") specified. The rest of the fields are the vals and vars. AnyVals are
 * serialized to JSON primitive types, Arrays to JSON arrays, AnyRefs to instances in the repository and singleton
 * objects to references. An example covering all the concepts follows:
 *
 * {{{
 * {
 *     value: { ref: 0 }, // The root object.
 *     objects: [ // Repository of all AnyRef objects.
 *         {
 *             $id: 0, // Id of the object, which is used while referencing it.
 *             $type: "my.package.X", // Type of the instance.
 *             a: "Foo", // AnyVal.
 *             b: { ref: 1 }, // Reference to an object.
 *             c: { ref: "scala.collection.immutable.Nil", // Reference to a singleton object.
 *             d: [ 123, 456, 789 ], // Array of AnyVals.
 *             e: [ { ref: 1 }, { ref: 2 } ] // Array of AnyRefs.
 *         },
 *         {
 *             $id: 1,
 *             // ...
 *         },
 *         {
 *            $id: 2,
 *            // ...
 *         }
 *         // ...
 *     ]
 * }
 * }}}
 *
 */
@swat.ignored
class JsonSerializer(val mirror: Mirror = runtimeMirror(getClass.getClassLoader)) {

    /**
     * Serializes the specified object to a JSON string that conforms to the specification described at
     * [[swat.common.json.JsonSerializer]].
     */
    def serialize(obj: Any): String = {
        var objectId = 0
        val classSymbols = mutable.HashMap[Class[_], Symbol]()
        val objectIds = mutable.HashMap[AnyRef, Int]()
        val serializedObjects = mutable.ListBuffer[JsObject]()

        // Internal serialization function.
        def serialize(obj: Any): JsValue = obj match {
            case null | () => JsNull
            case b: Boolean => JsBoolean(b)
            case b: Byte => JsNumber(b)
            case s: Short => JsNumber(s)
            case i: Int => JsNumber(i)
            case l: Long => JsNumber(l)
            case f: Float => JsNumber(f)
            case d: Double => JsNumber(d)
            case c: Char => JsString(c.toString)
            case s: String => JsString(s)
            case a: Array[_] => JsArray(a.map(serialize _))
            case a: AnyRef => serializeAnyRef(a)
            case x => throw new JsonException(s"Can't serialize value '$x'.")
        }

        // Internal serialization of AnyRefs.
        def serializeAnyRef(obj: AnyRef): JsObject = {
            val reference = objectIds.get(obj).map(JsNumber(_)).getOrElse {
                // The object hasn't been serialized yet.
                val objClass = obj.getClass
                val symbol = classSymbols.getOrElseUpdate(objClass, mirror.classSymbol(obj.getClass))
                if (symbol.isModuleClass) {
                    // Serialize singleton object as a reference to it's type full name.
                    JsString(symbol.fullName)
                } else {
                    // Assign the object a new id and serialize it.
                    val id = objectId
                    objectIds += obj -> id
                    objectId += 1
                    serializedObjects += serializeObject(obj, id, symbol)
                    JsNumber(id)
                }
            }

            JsObject(Seq("ref" -> reference))
        }

        // Internal serialization of objects using reflection.
        def serializeObject(obj: AnyRef, id: Int, symbol: Symbol): JsObject = {
            // Serialize all fields with getters.
            val members = symbol.typeSignature.members
            val getters = members.collect { case m: MethodSymbol if m.isGetter => m }
            val serializedFields = getters.map { getter =>
                val objMirror = mirror.reflect(obj)
                val getterMirror = objMirror.reflectMethod(getter)
                val value = getterMirror.apply()
                getter.name.toString -> serialize(value)
            }

            JsObject(Seq(
                "$id" -> JsNumber(id),
                "$type" -> JsString(symbol.fullName)
            ) ++ serializedFields)
        }

        Json.prettyPrint(JsObject(Seq(
            "value" -> serialize(obj),
            "objects" -> JsArray(serializedObjects)
        )))
    }

    /**
     * Deserializes the specified JSON string to an equivalent scala object. The JSON string has to conform to
     * specification described at [[swat.common.json.JsonSerializer]].
     */
    def deserialize(json: String): Any = Json.parse(json) match {
        case JsObject(Seq(("value", value), ("objects", objects: JsArray))) => {
            ???
        }
        case _ => throw new JsonException(s"Can't deserialize '$json', it doesn't conform to the specified structure.")
    }
}
