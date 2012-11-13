swat = {
    constructor: function(classes) {
        var constructor = function() {
            this.$init.apply(this, arguments);
        };
        swat.extend(constructor, classes[0]);
        constructor.prototype = {};

        var p = constructor.prototype;
        for (var i = 0; i < classes.length; i++) {
            swat.extend(p, classes[i]);
            p.prototype = {};
            p = p.prototype;
        }

        return constructor;
    },

    extend: function(target, source) {
        for (var k in source) {
            if (source.hasOwnProperty(k)) {
                target[k] = source[k];
            }
        }
    }
};

ScalaObject = {
    $init: function() {
        this.fields = {};
        this.params = {};
        alert("ScalaObject constructor");
    },
    $super: function(n) {
        n = n ? n : 1;
        var s = this;
        for (var i = 0; i < (n || 1); i++) {
            s = s.prototype;
        }
        return s;
    },
    hashCode: function() {
        return 42;
    }
}
ScalaObject = swat.constructor([ScalaObject]);

var x = new ScalaObject();
alert(x.hashCode());
