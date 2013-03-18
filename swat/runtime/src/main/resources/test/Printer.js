swat.provide('test.Printer');
swat.require('java.lang.Object', true);
swat.require('java.lang.String', false);
swat.require('scala.Any', true);
test.Printer.$init$ = (function() {
    var $self = this;
    swat.invokeSuper($self, '$init$', [], 'test.Printer');
});
test.Printer.print = swat.method('test.Printer.print', 'java.lang.String', (function(message) {
    var $self = this;
    console.log(message);
}));
test.Printer = swat.type('test.Printer', [test.Printer, java.lang.Object, scala.Any]);
