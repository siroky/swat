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
            function f1() { };
            function f2() { };
            function g1() {
                return 123;
            };
            function g2() {
                return 123;
            };
            function h(x, y) {
                return (x + y);
            };

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
