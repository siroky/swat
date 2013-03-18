swat.provide('java.lang.Object');

swat.require('scala.Any', true);

java.lang.Object.$init = function() {
    var $self = this;
    swat.invokeSuper($self, '$init$', [], 'java.lang.Object');
};
java.lang.Object = swat.type('java.lang.Object', [java.lang.Object, scala.Any]);
