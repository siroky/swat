swat.provide('B');
swat.require('A', true);
swat.require('Printer', false);
swat.require('java.lang.Object', true);
swat.require('java.lang.String', false);
swat.require('scala.Any', true);
B.$init$ = (function(y, x) {
    var $self = this;
    $super.$init$.call($self, x, 'java.lang.String');
    $self.$fields.y = y;
    swat.setParameter($self, 'x', x, B);
});
B.foo = swat.method('', (function() {
    var $self = this;
    var printer = new Printer();
    printer.print(($self.y() + swat.asInstanceOf($super.x.call($self), java.lang.String)), 'java.lang.String');
}));
B.y = swat.method('', (function() {
    var $self = this;
    return $self.$fields.y;
}));
B = swat.type('B', [B, A, java.lang.Object, scala.Any]);
