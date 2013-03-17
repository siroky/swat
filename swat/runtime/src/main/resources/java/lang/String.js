swat.provide('java.lang.String');
swat.require('java.lang.Object', true);
swat.require('scala.Any', true);
java.lang.String.$init$ = (function() {
    var $self = this;
    $super.$init$.call($self);
});
java.lang.String = swat.type('java.lang.String', [java.lang.String, java.lang.Object, scala.Any]);
