swat.provide('scala.Any');

// Every type requires the scala.Any, so this require ensures that the swat.js is always declared first.
swat.require('swat', true);
swat.require('java.lang.Class', true);

scala.Any.$init$ = function() {
    this.$fields = {};
    this.$params = {};
    this.$id = swat.newId();
};
scala.Any = swat.type('scala.Any', [scala.Any]);
