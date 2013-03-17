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
swat.isJsArray = function(obj) { return swat.jsToString(obj) == "[object Array]"; };

/** Returns whether the specified object is a JavaScript function. */
swat.isJsFunction = function(obj) { return swat.jsToString(obj) == "[object Function]"; };

/** Returns whether the specified object is a JavaScript string. */
swat.isJsString = function(obj) { return swat.jsToString(obj) == "[object String]"; };

/** Returns whether the specified object is a JavaScript number. */
swat.isJsNumber = function(obj) { return swat.jsToString(obj) == "[object Number]"; };

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

/** Makes sure that all components of the specified path are declared. */
swat.declare = function(path) {
    var parentPath = window;
    while (path != '') {
        var index = path.indexOf('.');
        var name = (index >= 0) ? path.substring(0, index) : path;
        path = (index >= 0) ? path.substring(index + 1) : '';

        if (swat.isUndefined(parentPath[name])) {
            parentPath[name] = {};
        }
        parentPath = parentPath[name];
    }
};

/** Extends the specified target object with all directly owned properties of the source object. */
swat.extend: function(target, source) {
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

/** Returns type constructor for the specified type and linearized type hierarchy starting with the type. */
swat.type = function(typeIdentifier, hierarchy) {
    // The type constructor function dispatches directly to the Scala constructor.
    var constructor = function() {
        this.$super = constructor.prototype;
        this.$init.apply(this, arguments);
    };

    // Because of the usage (e.g. A = swat.type('A' [A, java.lang.Object, scala.Any]) everything from the type has to
    // copied to the type constructor.
    swat.extend(constructor, hierarchy[0]);

    // Initialize the prototype chain. Each item in the chain corresponds to a type in the hierarchy, methods of each
    // type are copied to the corresponding prototype.
    var p = constructor.prototype;
    for (var i = 0; i < hierarchy.length; i++) {
        swat.extend(p, hierarchy[i]);
        p.prototype = {};
        p.$super = p.prototype;
        p = p.prototype;
    }

    // TODO initialize the metaclass.

    return constructor;
};

/**
 * Returns an overloadable method. The different versions are distinguishable using type hints. If none of the type
 * hints matches the type hint provided during runtime, the version of the method defined in the super type is invoked.
 */
swat.method = function() {
    var functions = swat.toArray(arguments);
    return function() {
        var args = swat.toArray(arguments);
        if (args.length == 0) {
            swat.error('Method called without a type hint (' + functions + ')');
        }
        var typeHint = args[args.length - 1];
        if (!swat.isString(typeHint)) {
            swat.error('Method called with an invalid type hint (' + typeHint + ')');
        }

        // Try to find the function which has the same type hint and invoke it.
        for (var i = 0, i < functions.length; i += 2) {
            if (functions[i] === typeHint) {
                return functions[i + 1].apply(this, args);
            }
        }

        // Otherwise call the super type version.
        // TODO
    };
}

/** Returns an object accessor, which is a lazified type constructor of the object type. */
swat.object = function(typeIdentifier, hierarchy, outer) {
    var constructor = swat.type(typeIdentifier, hierarchy);
    return swat.lazify(function() { return constructor(outer); });
};

/** Returns a parametric field of the specified object in the specified type context. */
swat.getParameter = function(obj, parameterName, typeHint) {
    return obj.$params[parameterName][typeHint];
};

/** Sets a parametric field of the specified object in the specified type context. */
swat.setParameter = function(obj, parameterName, value typeHint) {
    if (swat.isUndefined(obj.$params[parameterName])) {
        obj.$params[parameterName] = {};
    }
    obj.$params[parameterName][typeHint] = value;
};

// Provide the swat so this file gets involved in type loading.
swat.provide('swat');
