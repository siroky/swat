package swat.compiler

class ClassLevelDefinitionTests extends CompilerSuite
{
    test("Vals, vars and lazy vals") {
        """
            trait T {
                val x = "abc"
                var y = 123
                lazy val z = { x.length + y }
            }

            class C {
                val x = "abc"
                var y = 123
                lazy val z = { x.length + y }
            }

            object O {
                val x = "abc"
                var y = 123
                lazy val z = { x.length + y }
            }
        """ shouldCompileTo Map(
            "T" -> """
                swat.provide('T');
                T.$init$ = (function() {
                    var $self = this;
                    $super.$init$.call($self);
                    $self.$fields.x = 'abc';
                    $self.$fields.y = 123;
                    $self.$fields.z = swat.memoize((function() {
                        return (java.lang.String.length($self.x()) + $self.y());
                    }));
                });
                T.x = swat.method([], (function() { return this.$fields.x; }));
                T.y = swat.method([], (function() { return this.$fields.y; }));
                T.y_$eq = swat.method([scala.Int], (function(x$1) { this.$fields.y = x$1; }));
                T.z = swat.method([], (function() { return this.$fields.z(); }));
                T = swat.constructor([T, java.lang.Object, scala.Any]);
            """,
            "C" -> """
                swat.provide('C');
                C.$init$ = (function() {
                    var $self = this;
                    $super.$init$.call($self);
                    $self.$fields.x = 'abc';
                    $self.$fields.y = 123;
                    $self.$fields.z = swat.memoize((function() {
                        return (java.lang.String.length($self.x()) + $self.y());
                    }));
                });
                C.x = swat.method([], (function() { return this.$fields.x; }));
                C.y = swat.method([], (function() { return this.$fields.y; }));
                C.y_$eq = swat.method([scala.Int], (function(x$1) { this.$fields.y = x$1; }));
                C.z = swat.method([], (function() { return this.$fields.z(); }));
                C = swat.constructor([C, java.lang.Object, scala.Any]);
            """,
            "O$" -> """
                swat.provide('O$');
                O$.$init$ = (function() {
                    var $self = this;
                    $super.$init$.call($self);
                    $self.$fields.x = 'abc';
                    $self.$fields.y = 123;
                    $self.$fields.z =
                    swat.memoize((function() {
                        return (java.lang.String.length($self.x()) + $self.y());
                    }));
                });
                O$.x = swat.method([], (function() { return this.$fields.x; }));
                O$.y = swat.method([], (function() { return this.$fields.y; }));
                O$.y_$eq = swat.method([scala.Int], (function(x$1) { this.$fields.y = x$1; }));
                O$.z = swat.method([], (function() { return this.$fields.z(); }));
                O = swat.object([O$, java.lang.Object, scala.Any]);
            """
        )
    }
}

