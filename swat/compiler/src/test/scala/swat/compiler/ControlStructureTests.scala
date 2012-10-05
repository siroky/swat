package swat.compiler

class ControlStructureTests extends CompilerSuite
{
    test("Conditions (if-then-else)") {
        """
            val expr = true

            if (expr) {
                "x"
            }

            if (expr) {
                "x"
            } else {
                "y"
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
                    return 'x';
                } else {
                    return 'y';
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
                    return (function() {
                        if (expr) {
                            return 'y';
                        } else {
                            return 'z';
                        }
                    })();
                }
            })();
        """
    }

    test("Loops (while-do, do-while)") {
        """
            val expr = true

            while (true) {
                "x"
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
