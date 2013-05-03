swat.provide('java.lang.Object');

swat.require('scala.Any', true);

java.lang.Object.$init$ = function() {
    swat.invokeSuper(this, '$init$', [], 'java.lang.Object');
};
java.lang.Object.equals = function(that) {
    return this === that;
};
java.lang.Object.hashCode = function() {
    return swat.hashCode(this.$id);
};
java.lang.Object.toString = function() {
    return this.$class.typeIdentifier + '@' + this.$id;
};
java.lang.Object = swat.type('java.lang.Object', [java.lang.Object, scala.Any]);

// Class java.lang.Class extends the java.lang.Object, however it has to be usable before any other class due to the
// fact, that a class declaration requires java.lang.Class. This can be understood as breaking of the cycle: Class is a
// class that extends the Object, Object is a class therefore it must have corresponding Class. So type is assigned
// to java.lang.Class and all current usages of it are refreshed.
java.lang.Class = swat.type('java.lang.Class', [java.lang.Class, java.lang.Object, scala.Any]);
swat.reassignClass(scala.Any);
swat.reassignClass(java.lang.Object);
swat.reassignClass(java.lang.Class);
