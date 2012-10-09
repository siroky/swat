package swat.compiler

class ControlStructureTests extends CompilerSuite
{
    test("Block scope is preserved") {
        """
            val x = 1

            val y = {
                val x = 2
                x
            }

            val z = {
                "a"
                "b"
                val x = 3
                "c"
            }
        """ fragmentShouldCompileTo """
            var x = 1;

            var y = (function() {
                var x = 2;
                return x;
            })();

            var z = (function() {
                'a';
                'b';
                var x = 3;
                return 'c';
            })();
        """
    }

    test("Conditions (if-then-else)") {
        """
            val expr = true

            if (expr) {
                "x"
            }

            if (expr) {
                "x"
                "y"
                "z"
            } else {
                "a"
                "b"
                "c"
            }

            val a =
                if (expr) {
                    "x"
                } else {
                    "y"
                }

            val b =
                if (expr) {
                    "x"
                } else if (expr) {
                    "y"
                } else {
                    "z"
                }

        """ fragmentShouldCompileTo """
            var expr = true;

            (function() {
                if (expr) {
                    return 'x';
                }
            })();

            (function() {
                if (expr) {
                    'x';
                    'y';
                    return 'z';
                } else {
                    'a';
                    'b';
                    return 'c';
                }
            })();

            var a = (function() {
                if (expr) {
                    return 'x';
                } else {
                    return 'y';
                }
            })();

            var b = (function() {
                if (expr) {
                    return 'x';
                } else {
                    if (expr) {
                        return 'y';
                    } else {
                        return 'z';
                    }
                }
            })();
        """
    }

    test("Loops (while-do, do-while)") {
        """
            val expr = true

            while (true) {
                "x"
                "y"
                "z"
            }

            while (expr) {
                "x"
            }

            do {
                "x"
            } while (true)

            do {
                "x"
            } while (expr)
        """ fragmentShouldCompileTo """
            var expr = true;

            (function() {
                while (true) {
                    'x';
                    'y';
                    'z';
                }
            })();

            (function() {
                while (expr) {
                    'x';
                }
            })();

            (function() {
                do {
                    'x';
                } while (true)
            })();

            (function() {
                do {
                    'x';
                } while (expr)
            })();
        """
    }

   /* TODO depends on qualifiers, operators, etc.
   test("Match statement") {
        """
            val x: Any = null

            x match {
                case _ => println("x")
            }

            x match {
                case 1 => println("nope")
                case _ => println("c")
            }
        """ fragmentShouldCompileTo """

        """
    }*/

    test("Exceptions (throw, try-catch-finally)") {
        """
            throw new Exception("foo")

            val x = throw new Exception("foo")
        """ fragmentShouldCompileTo """
            (function() {
                throw new java.lang.Exception('foo');
            })();

            var x = (function() {
                throw new java.lang.Exception('foo');
            })();
        """
    }
}
