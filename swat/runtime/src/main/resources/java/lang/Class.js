swat.provide('java.lang.Class');

swat.require('swat', true);

// Just a dummy implementation instead of swat.type. See java.lang.Object for further information.
java.lang.Class = function(typeIdentifier, superTypes) {
    this.typeIdentifier = typeIdentifier;
    this.superTypes = superTypes;
};

java.lang.Class.$init$ = function(typeIdentifier, superTypes) {
    this.typeIdentifier = typeIdentifier;
    this.superTypes = superTypes;
};
