swat.provide('A');
swat.require('java.lang.Object', true);
swat.require('scala.Any', true);
A.$init$ = (function(x) {
    var $self = this;
    $super.$init$.call($self);
    $self.$fields.x = x;
});
A.x = swat.method('', (function() {
    var $self = this;
    return $self.$fields.x;
}));
A = swat.type('A', [A, java.lang.Object, scala.Any]);
