swat.provide('test.A');
swat.require('java.lang.Object', true);
swat.require('scala.Any', true);
test.A.$init$ = (function(x) {
    var $self = this;
    swat.invokeSuper($self, '$init$', [], 'test.A');
    $self.$fields.x = x;
});
test.A.x = swat.method('test.A.x', '', (function() {
    var $self = this;
    return $self.$fields.x;
}));
test.A = swat.type('test.A', [test.A, java.lang.Object, scala.Any]);
