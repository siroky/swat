package swat.compiler

class LocalDefinitionTests extends CompilerSuite {

    test("Vals") {
        """
            val x = "abc"
            val y = 123
            
            x
            y
        """ fragmentShouldCompileTo """
            var x = 'abc';
            var y = 123;
            
            x;
            y;
        """
    }

    test("Vars") {
        """
            var x = "abc"
            var y = 123
            
            x
            y
        """ fragmentShouldCompileTo """
            var x = 'abc';
            var y = 123;
            
            x;
            y;
        """
    }

    test("Lazy vals") {
        """
            lazy val x = 123
            lazy val y = 456
            lazy val z = { x + y + 789 }

            x
            y
            z
        """ fragmentShouldCompileTo """
            var x = swat.lazify((function() { return 123; }));
            var y = swat.lazify((function() { return 456; }));
            var z = swat.lazify((function() { return ((x() + y()) + 789); }));

            x();
            y();
            z();
        """
    }

    test("Pattern vals") {
        """
            val x: Any = null
            val (a, b, c) = x
            val Some(g: String) = x
        """ fragmentShouldCompileTo """
            var x = null;
            var x$1 = (function() { var x1 = x;
                function case5() {
                    if (swat.isInstanceOf(x1, scala.Tuple3)) {
                        var x2 = swat.asInstanceOf(x1, scala.Tuple3);
                        return (function() {
                            var a = x2._1();
                            var b = x2._2();
                            var c = x2._3();
                            return matchEnd4(new scala.Tuple3(a, b, c));
                        })();
                    } else {
                        return case6();
                    }
                };
                function case6() {
                    return matchEnd4((function() {
                        throw new scala.MatchError(x1);
                    })());
                };
                function matchEnd4(x) { return x; };
                return case5();
            })();
            var a = x$1._1();
            var b = x$1._2();
            var c = x$1._3();
            var g = (function() {
                var x1 = x;
                function case6() {
                    if (swat.isInstanceOf(x1, scala.Some)) {
                        var x2 = swat.asInstanceOf(x1, scala.Some);
                        return (function() {
                            var g = x2.x();
                            return (function() {
                                if (swat.isInstanceOf(g, java.lang.String)) {
                                    var x3 = swat.asInstanceOf(g, java.lang.String);
                                    return matchEnd5(x3);
                                } else {
                                    return case7();
                                }
                            })();
                        })();
                    } else {
                        return case7();
                    }
                };
                function case7() {
                    return matchEnd5((function() {
                        throw new scala.MatchError(x1);
                    })());
                };
                function matchEnd5(x) { return x; };
                return case6();
            })();
                                    """
    }

    test("Defs") {
        """
            def f1 { }
            def f2() { }
            def g1 = 123
            def g2() = 123
            def h(x: Int, y: Int) = x + y

            f1
            f2
            f2()
            g1
            g2
            g2()
            h(1, 2)
        """ fragmentShouldCompileTo """
            var f1 = (function() { });
            var f2 = (function() { });
            var g1 = (function() {
                return 123;
            });
            var g2 = (function() {
                return 123;
            });
            var h = (function(x, y) {
                return (x + y);
            });

            f1();
            f2();
            f2();
            g1();
            g2();
            g2();
            h(1, 2);
        """
    }

    test("Functions") {
        """
            val f = (x: Int, y: Int) => x * y

            val g = (x: Int) => (y: Int) => x + y

            val h = f(_, _)
            val i = h(_, _)
            val j = f(1, _: Int)
            val k = f(_: Int, 1)
        """ fragmentShouldCompileTo """
            var f = (function(x, y) {
                return (x * y);
            });

            var g = (function(x) {
                return (function(y) {
                    return (x + y);
                });
            });

            var h = (function(x$1, x$2) {
                return f(x$1, x$2);
            });
            var i = (function(x$3, x$4) {
                return h(x$3, x$4);
            });
            var j = (function(x$5) {
                return f(1, x$5);
            });
            var k = (function(x$6) {
                return f(x$6, 1);
            });
        """
    }

    test("Classes and Anonymous classes") {
        """
            trait T
            trait U
            class C extends T
            object O

            val c = new C
            val o = O

            val a = new C with U
            val b = new {
                val x = 10
                val y = 20
            }
        """ fragmentShouldCompileTo """
            T = swat.type('T', [T, java.lang.Object, scala.Any]);
            U = swat.type('U', [U, java.lang.Object, scala.Any]);

            C.$init$ = (function($outer) {
                var $self = this;
                swat.invokeSuper($self, '$init$', [], 'C');
                $self.$outer = $outer;
            });
            C = swat.type('C', [C, T, java.lang.Object, scala.Any]);

            O$.$init$ = (function($outer) {
                var $self = this;
                swat.invokeSuper($self, '$init$', [], 'O$');
                $self.$outer = $outer;
            });
            O$ = swat.object('O$', [O$, java.lang.Object, scala.Any], $self);

            var c = new C($self);
            var o = O$();

            var a = (function() {
                $anon.$init$ = (function($outer) {
                    var $self = this;
                    swat.invokeSuper($self, '$init$', [$outer, 'A'], '$anon');
                    $self.$outer = $outer;
                });
                $anon = swat.type('$anon', [$anon, U, C, T, java.lang.Object, scala.Any]);
                return new $anon($self);
            })();

            var b = (function() {
                $anon.$init$ = (function($outer) {
                    var $self = this;
                    swat.invokeSuper($self, '$init$', [], '$anon');
                    $self.$fields.x = 10;
                    $self.$fields.y = 20;
                    $self.$outer = $outer;
                });
                $anon.x = swat.method('$anon.x', '', (function() { var $self = this; return $self.$fields.x; }));
                $anon.y = swat.method('$anon.y', '', (function() { var $self = this; return $self.$fields.y; }));
                $anon = swat.type('$anon', [$anon, java.lang.Object, scala.Any]);
                return new $anon($self);
            })();
        """
    }
}
