swat.provide('Printer');
swat.require('java.lang.Object', true);
swat.require('java.lang.String', false);
swat.require('scala.Any', true);
Printer.$init$ = (function() {
    var $self = this;
    $super.$init$.call($self);
});
Printer.print = swat.method([java.lang.String], (function(message) {
    var $self = this;
    console.log(message);
}));
Printer = swat.type('Printer', [Printer, java.lang.Object, scala.Any]);
