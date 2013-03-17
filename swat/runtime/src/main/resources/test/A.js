swat.provide('test.A');
swat.require('java.lang.Object', true);
swat.require('scala.Any', true);
test.A.$init$ = (function(x) {
    var $self = this;
    $super.$init$.call($self);
    $self.$fields.x = x;
});
test.A.x = swat.method('', (function() {
    var $self = this;
    return $self.$fields.x;
}));
test.A = swat.type('test.A', [test.A, java.lang.Object, scala.Any]);
