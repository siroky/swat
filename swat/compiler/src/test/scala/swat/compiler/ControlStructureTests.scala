package swat.compiler

class ControlStructureTests extends CompilerSuite {

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
                val z = {
                    val z = "foo"
                }
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
                var z = (function() {
                    var z = 'foo';
                })();
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

    test("Match statement") {
        """
            val x: Any = null

            val y = x match {
                case "foo" => "bar"
                case Some("bar") => "bar"
                case _ => "baz"
            }
        """ fragmentShouldCompileTo """
            var x = null;
            var y = (function() {
                var x1 = x;
                function case8() {
                    if (('foo' === x1)) { return matchEnd7('bar'); }
                    else { return case9(); }
                };
                function case9() {
                    if (swat.isInstanceOf(x1, scala.Some)) {
                        var x3 = swat.asInstanceOf(x1, scala.Some);
                        return (function() {
                            var p2 = x3.x();
                            return (function() {
                                if (('bar' === p2)) { return matchEnd7('bar'); }
                                else { return case10(); }
                            })();
                        })();
                    } else { return case10(); }
                };
                function case10() { return matchEnd7('baz'); };
                function matchEnd7(x) { return x; };
                return case8();
            })();
                                    """
    }

    test("Throw statement") {
        """
            throw new Exception("foo")

            val x = throw new Exception("foo")
        """ fragmentShouldCompileTo """
            (function() {
                throw new java.lang.Exception('foo', 'java.lang.String');
            })();

            var x = (function() {
                throw new java.lang.Exception('foo', 'java.lang.String');
            })();
        """
    }

    test("Exception handling (try-catch-finally)") {
        """
            try {
                "foo"
            }

            try {
                "bar"
            } catch {
                case _: RuntimeException => println("runtime exception")
            }

            try {
                "bar"
            } finally {
                "baz"
            }

            try {
                "bar"
            } catch {
                case r: RuntimeException => println("runtime exception")
                case e: Exception => println("exeption")
                case _: Throwable => println("unknown exception")
            }

            try {
                "bar"
            } catch {
                case e: Exception if e.getMessage == "foo" => println("exception with foo message")
            }

            try {
                "bar"
            } catch {
                case e: Exception if e.getMessage == "foo" => println("exception with foo message")
                case _: Throwable => println("unknown exception")
            }

            try {
                "baz"
            } catch {
                case _: Throwable => println("unknown exception")
            } finally {
                "cleanup"
            }

            val x =
                try {
                    "success"
                } catch {
                    case _: Throwable => "unknown exception"
                } finally {
                    "z"
                }
        """ fragmentShouldCompileTo """
            'foo';

            (function() {
                try {
                    return 'bar';
                } catch (e$1) {
                    if (swat.isInstanceOf(e$1, java.lang.RuntimeException)) {
                        scala.Predef$().println('runtime exception', 'scala.Any');
                        return;
                    }
                    throw e$1;
                }
            })();

            (function() {
                try {
                    return 'bar';
                } finally {
                    'baz';
                }
            })();

            (function() {
                try {
                    return 'bar';
                } catch (e$2) {
                    if (swat.isInstanceOf(e$2, java.lang.RuntimeException)) {
                        var r = e$2;
                        scala.Predef$().println('runtime exception', 'scala.Any');
                        return;
                    }
                    if (swat.isInstanceOf(e$2, java.lang.Exception)) {
                        var e = e$2;
                        scala.Predef$().println('exeption', 'scala.Any');
                        return;
                    }
                    if (swat.isInstanceOf(e$2, java.lang.Throwable)) {
                        scala.Predef$().println('unknown exception', 'scala.Any');
                        return;
                    }
                    throw e$2;
                }
            })();

            (function() {
                try {
                    return 'bar';
                } catch (ex6) {
                    var x4 = ex6;
                    function case9() {
                        if (swat.isInstanceOf(x4, java.lang.Exception)) {
                            var x5 = swat.asInstanceOf(x4, java.lang.Exception);
                            return (function() {
                                if ((x5.getMessage() === 'foo')) {
                                    return matchEnd8(scala.Predef$().println('exception with foo message', 'scala.Any'));
                                } else {
                                    return case10();
                                }
                            })();
                        } else {
                            return case10();
                        }
                    };
                    function case10() {
                        return matchEnd8((function() {
                            throw ex6;
                        })());
                    };
                    function matchEnd8(x) { return x; };
                    return case9();
                }
            })();

            (function() {
                try {
                    return 'bar';
                } catch (ex9) {
                    var x6 = ex9;
                    function case12() {
                        if (swat.isInstanceOf(x6, java.lang.Exception)) {
                            var x7 = swat.asInstanceOf(x6, java.lang.Exception);
                            return (function() {
                                if ((x7.getMessage() === 'foo')) {
                                    return matchEnd11(scala.Predef$().println('exception with foo message', 'scala.Any'));
                                } else {
                                    return case13();
                                }
                            })();
                        } else {
                            return case13();
                        }
                    };
                    function case13() {
                        if ((x6 !== null)) {
                            return matchEnd11(scala.Predef$().println('unknown exception', 'scala.Any'));
                        } else { return case14(); }
                    };
                    function case14() { return matchEnd11((function() { throw ex9; })()); };
                    function matchEnd11(x) { return x; };
                    return case12();
                }
            })();

            (function() {
                try { return 'baz'; }
                catch (e$3) {
                    if (swat.isInstanceOf(e$3, java.lang.Throwable)) {
                        scala.Predef$().println('unknown exception', 'scala.Any');
                        return;
                    }
                    throw e$3;
                } finally { 'cleanup'; }
            })();

            var x = (function() {
                try { return 'success'; }
                catch (e$4) {
                    if (swat.isInstanceOf(e$4, java.lang.Throwable)) {
                        return 'unknown exception';
                    }
                    throw e$4;
                } finally { 'z'; }
            })();
                                    """
    }
}
