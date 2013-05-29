swat = {};

/** Url of the Swat controller. */
swat.controllerUrl = '/swat';

/** Returns whether the specified object is the undefined value. */
swat.isUndefined = function(obj) { return typeof obj === 'undefined'; };

/** Returns whether the specified object is not the undefined. */
swat.isDefined = function(obj) { return !swat.isUndefined(obj); };

/** Returns native JavaScript string representation of the specified object. */
swat.jsToString = function(obj) { return Object.prototype.toString.call(obj); };

/** Returns whether the specified object is a JavaScript object. */
swat.isJsObject = function(obj) { return obj === Object(obj); };

/** Returns whether the specified object is a JavaScript array. */
swat.isJsArray = function(obj) { return swat.jsToString(obj) == '[object Array]'; };

/** Returns whether the specified object is a JavaScript function. */
swat.isJsFunction = function(obj) { return swat.jsToString(obj) == '[object Function]'; };

/** Returns whether the specified object is a JavaScript string. */
swat.isJsString = function(obj) { return swat.jsToString(obj) == '[object String]'; };

/** Returns whether the specified object is a JavaScript number. */
swat.isJsNumber = function(obj) { return swat.jsToString(obj) == '[object Number]'; };

/** Returns whether the specified object is a JavaScript boolean. */
swat.isJsBoolean = function(obj) { return obj === true || obj === false || swat.jsToString(obj) == '[object Boolean]'; };

/** Returns whether the specified object can be perceived as an integer. */
swat.isInteger = function(obj) { return swat.isJsNumber(obj) && (obj % 1 === 0); };

/** Returns whether the specified object can be perceived as one character. */
swat.isChar = function(obj) { return swat.isString(obj) && obj.length === 1; };

/** Turns an array-like object into an array. */
swat.toArray = function(obj) { return Array.prototype.slice.call(obj); };

/** Reports a fatal error. */
swat.error = function(message) {
    console.log(message);
};

/** Traverses all objects on the specified path. The visitor function gets an owning object and name of the field. */
swat.traverse = function(path, visitor) {
    var parent = window;
    while (path != '') {
        var index = path.indexOf('.');
        var name = (index >= 0) ? path.substring(0, index) : path;
        path = (index >= 0) ? path.substring(index + 1) : '';
        visitor(parent, name);
        parent = parent[name];
    }
};

/** Makes sure that all objects on the specified path are declared. */
swat.declare = function(path) {
    swat.traverse(path, function(parent, name) {
        if (swat.isUndefined(parent[name])) {
            parent[name] = {};
        }
    });
};

/** Returns an object corresponding to the path. */
swat.access = function(path) {
    var result = undefined;
    swat.traverse(path, function(parent, name) { result = parent[name]; });
    return result;
};

/** Extends the specified target object with all directly owned properties of the source object. */
swat.extend = function(target, source) {
    for (var k in source) {
        if (source.hasOwnProperty(k)) {
            target[k] = source[k];
        }
    }
};

/** The types that are currently loaded. Filled by the swat.provide method. */
swat.loadedTypes = [];

/** Checks whether the required type has already been provided in case it's a hard dependency. */
swat.require = function(typeIdentifier, isHard) {
    if (isHard && swat.loadedTypes.indexOf(typeIdentifier) < 0) {
        swat.error('Required type (' + typeIdentifier + ') has not been provided yet.');
    }
};

/** Marks the specified type as provided also creates it's package if it doesn't already exist. */
swat.provide = function(typeIdentifier) {
    if (swat.loadedTypes.indexOf(typeIdentifier) < 0) {
        swat.loadedTypes.push(typeIdentifier);
        swat.declare(typeIdentifier);
    }
};

/**
 * Lazifies the specified function. I.e. when it's invoked for the first time, the f is invoked and the result is
 * stored. For each succeeding invocation, the stored result is returned.
 */
swat.lazify = function(f) {
    var l = function() {
        if (swat.isUndefined(l.$result)) {
            l.$result = f.apply(this, arguments);
        }
        return l.$result;
    };
    return l;
};

/** Converts the specified function literal f to a FunctionN-like object. */
swat.func = function(arity, f) {
    f.$arity = arity;
    // TODO assign $class.
    return f;
};

/**
 * Creates a prototype object for the specified type hierarchy while chaining the prototypes in a way that
 * Sub <: Super => SubPrototype.prototype == SuperPrototype. Initializes both the internal prototype links that are
 * used during method resolution and explicit prototype links via '$prototype' field so the prototype hierarchy can
 * be traversed programatically.
 */
swat.typePrototype = function(hierarchy) {
    var prototype = {};
    for (var i = hierarchy.length - 1; i >= 0; i--) {
        // Create a new object (i.e. child prototype) with the current prototype as its prototype.
        var childConstructor = function() {};
        childConstructor.prototype = prototype;
        var childPrototype = new childConstructor();
        childPrototype.$prototype = prototype;

        // Extend the child prototype with its methods and continue.
        swat.extend(childPrototype, hierarchy[i]);
        prototype = childPrototype;
    }
    return prototype;
};

/** Assigns meta class to the specified type. */
swat.assignClass = function(type, typeIdentifier, superTypes) {
    var metaClass = new java.lang.Class(typeIdentifier, superTypes);
    type.$class = metaClass;
    type.prototype.$class = metaClass;
};

/** Refreshes meta class of the specified type. */
swat.reassignClass = function(type) {
    swat.assignClass(type, type.$class.typeIdentifier, type.$class.superTypes);
};

/** Returns type constructor for the specified type and linearized type hierarchy starting with the type. */
swat.type = function(typeIdentifier, hierarchy) {
    var prototype = swat.typePrototype(hierarchy);

    // The type constructor function dispatches directly to the Scala constructor.
    var typeConstructor = function() {
        this.$prototype = prototype;
        this.$init$.apply(this, arguments);
    };
    typeConstructor.prototype = prototype;
    prototype.constructor = typeConstructor;

    // Because of the usage (e.g. A = swat.type('A' [A, java.lang.Object, scala.Any]) everything from the type has to
    // copied to the type constructor.
    swat.extend(typeConstructor, hierarchy[0]);

    // Set the meta class of the type.
    swat.assignClass(typeConstructor, typeIdentifier, hierarchy.splice(1));

    return typeConstructor;
};

/** Returns an object accessor, which is a lazified type constructor of the object type. */
swat.object = function(typeIdentifier, hierarchy, outer) {
    var constructor = swat.type(typeIdentifier, hierarchy, true);
    constructor.$class.isSingleton = true;
    return swat.lazify(function() { return new constructor(outer); });
};

/**
 * Returns an overloadable method. The different versions are distinguishable using type hints. If none of the type
 * hints matches the type hint provided during runtime, the version of the method defined in the super type is invoked.
 */
swat.method = function(methodIdentifier) {
    var overloads = swat.toArray(arguments).splice(1);
    var index = methodIdentifier.lastIndexOf('.');
    var typeIdentifier = methodIdentifier.substring(0, index);
    var methodName = methodIdentifier.substring(index + 1);

    return function() {
        var args = swat.toArray(arguments);
        var typeHint = args[args.length - 1] || '';
        if (!swat.isJsString(typeHint)) {
            swat.error('Method called with an invalid type hint (' + typeHint + ')');
        }

        // Try to find the function which has the same type hint and invoke it.
        for (var i = 0; i < overloads.length; i += 2) {
            if (overloads[i] === typeHint) {
                return overloads[i + 1].apply(this, args);
            }
        }

        // Otherwise call the super type version.
        return swat.invokeSuper(this, methodName, args, typeIdentifier);
    };
};

/** Returns a new unique id. */
swat.newId = function() {
    swat.newId.counter++;
    return swat.newId.counter;
};
swat.newId.counter = 0;

/** Invokes the specified method. */
swat.invoke = function(obj, method, methodName, args, typeIdentifier) {
    if (swat.isUndefined(method)) {
        swat.error('Cannot invoke method ' + methodName + ' in type ' + typeIdentifier + '.');
    }
    return method.apply(obj, args);
};

/** Invokes the specified method defined directly on the specified type. */
swat.invokeThis = function(obj, methodName, args, typeIdentifier) {
    return swat.invoke(obj, swat.access(typeIdentifier)[methodName], methodName, args, typeIdentifier);
};

/** Invokes super version of the specified method defined above the specified type in the inheritance hierarchy. */
swat.invokeSuper = function(obj, methodName, args, typeIdentifier, superTypeIdentifier) {
    if (swat.isDefined(superTypeIdentifier)) {
        // The type where the method can be found is specified.
        return swat.invoke(obj, swat.access(superTypeIdentifier)[methodName], methodName, args, typeIdentifier);
    }

    // First method with the same name has to be found in the prototype hierarchy above the typeIdentifier.
    var method = undefined;
    var visitedType = false;
    var p = obj.$prototype;
    while (swat.isDefined(p)) {
        if (visitedType && p.hasOwnProperty(methodName)) {
            method = p[methodName];
            break;
        } else if (p.$class.typeIdentifier == typeIdentifier) {
            visitedType = true;
        }
        p = p.$prototype;
    }

    return swat.invoke(obj, method, methodName, args, typeIdentifier);
};

/** Invokes the specified method via RPC using the client proxy. */
swat.invokeRemote = function(methodName, args) {
    return rpc.RpcProxy$().invoke(methodName, args, 'java.lang.String, scala.Product');
};

/** Returns a parametric field of the specified object in the specified type context. */
swat.getParameter = function(obj, parameterName, typeHint) {
    return obj.$params[parameterName][typeHint];
};

/** Sets a parametric field of the specified object in the specified type context. */
swat.setParameter = function(obj, parameterName, value, typeHint) {
    if (swat.isUndefined(obj.$params[parameterName])) {
        obj.$params[parameterName] = {};
    }
    obj.$params[parameterName][typeHint] = value;
};

/** Returns meta class of the specified type (instance of java.lang.Class). */
swat.classOf = function(type) {
    return type.$class;
};

/** Returns whether the specified object is an instance of swat type. */
swat.isSwatObject = function(obj) {
    return swat.isDefined(obj) && obj !== null && swat.isDefined(obj.$class);
};

/** Native implementation of the Scala isInstanceOf method. */
swat.isInstanceOf = function(obj, type) {
    if (swat.isUndefined(obj) || obj == null) {
        return false;
    }

    var typeIdentifier = type.$class.typeIdentifier;
    var typeIs = function(i) { return typeIdentifier === i; };
    var typeIsAny = typeIs('scala.Any');
    var typeIsAnyOrAnyVal = typeIsAny || typeIs('scala.AnyVal');
    var typeIsAnyOrObject = typeIsAny || typeIs('java.lang.Object');

    if (swat.isJsNumber(obj)) {
        var typeIsIntegral = ['scala.Byte', 'scala.Short', 'scala.Int', 'scala.Long'].indexOf(typeIdentifier) > 0;
        var typeIsFloating = ['scala.Float', 'scala.Double'].indexOf(typeIdentifier) > 0;
        if (typeIsAnyOrAnyVal || typeIsFloating || (typeIsIntegral && swat.isInteger(obj))) {
            return true;
        }
    } else if (swat.isJsBoolean(obj) && (typeIsAnyOrAnyVal || typeIs('scala.Boolean'))) {
        return true;
    } else if (swat.isJsString(obj) && (typeIsAnyOrObject || typeIs('java.lang.String') || (typeIs('scala.Char') && swat.isChar(obj)))) {
        return true;
    } else if (swat.isSwatObject(obj)) {
        if (typeIsAnyOrObject || obj.$class.typeIdentifier === type.$class.typeIdentifier) {
            return true;
        } else if (swat.isSubTypeOf(obj.$class, type.$class)) {
            return true;
        }
    }

    return false;
};

/** Native implementation of the Scala asInstanceOf method. */
swat.asInstanceOf = function(obj, type) {
    if (swat.isInstanceOf(obj, type) || (obj === null && swat.isSubTypeOf(type.$class, java.lang.Object.$class))) {
        return obj;
    }

    var identifier = swat.jsToString(obj);
    if (swat.isSwatObject(obj)) {
        identifier = obj.$class.typeIdentifier;
    }
    var message = 'The object of type ' + identifier + ' cannot be cast to ' + type.$class.typeIdentifier + '.'
    throw new java.lang.ClassCastException(message, 'java.lang.String');
};

/** Returns whether the specified type is subtype of the specified super type. */
swat.isSubTypeOf = function(typeClass, superTypeClass) {
    var superTypes = typeClass.superTypes;
    for (var i = 0; i < superTypes.length; i++) {
        if (superTypes[i].$class.typeIdentifier === superTypeClass.typeIdentifier) {
            return true;
        }
    }
};

/** Throws a NullPointerException iff the specified object is null or undefined. */
swat.throwIfNull = function(obj) {
    if (swat.isUndefined(obj) || obj == null) {
        throw new java.lang.NullPointerException();
    }
};

/** Native implementation of the Scala equals method. */
swat.equals = function(obj1, obj2) {
    if (swat.isUndefined(obj1) || swat.isUndefined(obj1)) {
        return false;
    } else if (obj1 === obj2) {
        return true;
    } else if (swat.isJsFunction(obj1)) {
        return obj1 === obj2;
    } else if (swat.isSwatObject(obj1)) {
        return obj1.equals(obj2, 'scala.Any');
    }
    return false;
};

/** Returns meta class of the specified type (instance of java.lang.Class). */
swat.getClass = function(obj) {
    swat.throwIfNull(obj);

    // Doesn't work reliably for primitive types.
    if (swat.isJsBoolean(obj)) {
        return scala.Boolean.$class;
    } else if (swat.isInteger(obj)) {
        return scala.Int.$class;
    } else if (swat.isJsNumber(obj)) {
        return scala.Double.$class;
    } else if (swat.isJsString(obj)) {
        return scala.String.$class;
    }

    return obj.$class;
};

/** Native implementation of the Scala hashCode method. */
swat.hashCode = function(obj) {
    var code = 0;
    if (swat.isJsBoolean(obj)) {
        code = obj ? 1231 : 1237;
    } else if (swat.isJsNumber(obj)) {
        code = obj;
    } else if (swat.isJsString(obj)) {
        for (var i = 0; i < obj.length; i++) {
            code += obj.charCodeAt(i) * (31 ^ obj.length - 1 - i);
            code %= 2147483647;
        }
    } else if (swat.isJsFunction(obj)) {
        return swat.hashCode(obj.toString());
    } else if (swat.isSwatObject(obj)) {
        return obj.hashCode();
    }
    return Math.round(code) % 2147483647;
};

/** Native implementation of the Scala toString method. */
swat.toString = function(obj) {
    swat.throwIfNull(obj);

    if (swat.isJsFunction(obj)) {
        return '<function' + obj.$arity + '>';
    } else {
        return obj.toString();
    }
};

/* Serializes the specified value to a Swat serialization format described in the server-side serializer. */
swat.serialize = function(value) {
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
        var jsArray = value.jsArray();
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
};

/** Turns a JavaScript array into a scala.Array. */
swat.jsArrayToScalaArray = function(array) { return scala.Array$().apply(array, 'Array'); };

// Provide the swat so this file gets involved in type loading.
swat.provide('swat');

// The internal classes that are always required by the swat runtime.
swat.require('scala.Any', false);
swat.require('scala.AnyVal', false);
swat.require('scala.Boolean', false);
swat.require('scala.Byte', false);
swat.require('scala.Char', false);
swat.require('scala.Double', false);
swat.require('scala.Float', false);
swat.require('scala.Int', false);
swat.require('scala.Long', false);
swat.require('scala.Short', false);
swat.require('scala.Array', false);
swat.require('scala.Array$', false);
swat.require('java.lang.Object', false);
swat.require('java.lang.String', false);
swat.require('java.lang.Class', false);
swat.require('java.lang.ClassCastException', false);
swat.require('java.lang.NullPointerException', false);
