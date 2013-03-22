swat = {};

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
swat.isInteger = function(obj) { return swat.isNumber(obj) && (obj % 1 === 0); };

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

// The types that are currently loaded.
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
    var constructor = swat.type(typeIdentifier, hierarchy);
    return swat.lazify(function() { return constructor(outer); });
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
        if (args.length == 0) {
            swat.error('Method called without a type hint (' + overloads + ')');
        }
        var typeHint = args[args.length - 1];
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

/** Invokes super version of the specified method defined above the specified type in the inheritance hierarchy. */
swat.invokeSuper = function(obj, methodName, args, typeIdentifier, superTypeIdentifier) {
    var method = undefined;
    if (swat.isDefined(superTypeIdentifier)) {
        // The type where the method can be found is specified.
        method = swat.access(superTypeIdentifier)[methodName];
    } else {
        // First method with the same name has to be found in the prototype hierarchy above the typeIdentifier.
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
    }

    // Invoke the method.
    if (swat.isDefined(method)) {
        return method.apply(obj, args);
    } else {
        swat.error('Cannot invoke super method ' + methodName + ' in type ' + typeIdentifier + '.')
    }
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
    } else if (swat.isJsObject(obj)) {
        if (typeIsAnyOrObject) {
            return true;
        } else if (swat.isDefined(obj.$class)) {
            // Check whether any of the object super types is actually the checked type.
            var superTypes = obj.$class.superTypes;
            for (var i = 0; i < superTypes.length; i++) {
                if (superTypes[i].$class.typeIdentifier === typeIdentifier) {
                    return true;
                }
            }
        }
    }

    return false;
};

// Provide the swat so this file gets involved in type loading.
swat.provide('swat');
