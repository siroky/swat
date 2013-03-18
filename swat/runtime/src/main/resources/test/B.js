swat.provide('test.B');
swat.require('java.lang.Object', true);
swat.require('java.lang.String', false);
swat.require('scala.Any', true);
swat.require('test.A', true);
swat.require('test.Printer', false);
test.B.$init$ = (function(y, x) {
    var $self = this;
    swat.invokeSuper($self, '$init$', [x, 'java.lang.String'], 'test.B');
    $self.$fields.y = y;
    swat.setParameter($self, 'x', x, 'test.B');
});
test.B.foo = swat.method('test.B.foo', '', (function() {
    var $self = this;
    var printer = new test.Printer();
    printer.print(($self.y() + swat.asInstanceOf(swat.invokeSuper($self, 'x', [], 'test.B'), java.lang.String)), 'java.lang.String');
}));
test.B.y = swat.method('test.B.y', '', (function() {
    var $self = this;
    return $self.$fields.y;
}));
test.B = swat.type('test.B', [test.B, test.A, java.lang.Object, scala.Any]);
