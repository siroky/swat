swat.provide('swat.runtime.loader.Test');
swat.runtime.loader.Test.$init$ = (function(x) {
    var $self = this;
    $super.$init$.call($self);
    $self.$fields.x = x;
});
swat.runtime.loader.Test.x = swat.method([], (function() {
    var $self = this;
    return $self.$fields.x;
}));
swat.runtime.loader.Test = swat.type([swat.runtime.loader.Test, java.lang.Object, scala.Any]);
