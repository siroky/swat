package swat.compiler

class LocalDefinitionTests extends CompilerSuite
{
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
            var x = swat.memoize((function() { return 123; }));
            var y = swat.memoize((function() { return 456; }));
            var z = swat.memoize((function() { return ((x() + y()) + 789); }));

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
                            if ((x2 !== null)) {
                                var a = x2._1();
                                var b = x2._2();
                                var c = x2._3();
                                return matchEnd4(new scala.Tuple3(a, b, c));
                            } else {
                                return case6();
                            }
                        })();
                    } else { return case6(); }
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
                            if ((x2 !== null)) {
                                var g = x2.x();
                                return (function() {
                                    if (swat.isInstanceOf(g, java.lang.String)) {
                                        var x3 = swat.asInstanceOf(g, java.lang.String);
                                        return matchEnd5(x3);
                                    } else { return case7(); }
                                })();
                            } else { return case7(); }
                        })();
                    } else { return case7(); }
                };
                function case7() {
                    return matchEnd5((function() { throw new scala.MatchError(x1); })());
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
}
