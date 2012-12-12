package swat.compiler

class ClassLevelDefinitionTests extends CompilerSuite
{
    test("Constructors") {
        """
            class A {
                println("Foo")
            }

            class B(val foo: String, val bar: Int) {
                val baz: Boolean = true
            }

            class C(foo: String, bar: Int) {
                def baz = foo
            }

            class D(val foo: Int) {
                def this() = {
                    this(0)
                    println("Foo")
                }
                def this(foo: String) = this(foo.length)
                def this(foo: Boolean) = this(if (foo) "true" else "false")
            }
        """ shouldCompileTo Map(
            "A" -> """
                swat.provide('A');
                A.$init$ = (function() {
                    var $self = this;
                    $super.$init$.call($self);
                    scala.Predef.println('Foo', [scala.Any]);
                });
                A = swat.constructor([A, java.lang.Object, scala.Any]);
                   """,
            "B" -> """
                swat.provide('B');
                B.$init$ = (function(foo, bar) {
                    var $self = this;
                    $super.$init$.call($self);
                    $self.$fields.foo = foo;
                    $self.$fields.bar = bar;
                    $self.$fields.baz = true;
                });
                B.bar = swat.method([], (function() { var $self = this; return $self.$fields.bar; }));
                B.baz = swat.method([], (function() { var $self = this; return $self.$fields.baz; }));
                B.foo = swat.method([], (function() { var $self = this; return $self.$fields.foo; }));
                B = swat.constructor([B, java.lang.Object, scala.Any]);
                   """,
            "C" -> """
                swat.provide('C');
                C.$init$ = (function(foo, bar) {
                    var $self = this;
                    $super.$init$.call($self);
                    swat.setParameter($self, 'foo', foo, C);
                    swat.setParameter($self, 'bar', bar, C);
                });
                C.baz = swat.method([], (function() { var $self = this; return swat.getParameter($self, 'foo', C); }));
                C = swat.constructor([C, java.lang.Object, scala.Any]);
                   """,
            "D" -> """
                swat.provide('D');
                D.$init$ = swat.method([scala.Int],
                    (function(foo) {
                        var $self = this;
                        $super.$init$.call($self);
                        $self.$fields.foo = foo;
                    }),
                    [], (function() {
                        var $self = this;
                        $self.$init$(0, [scala.Int]);
                        scala.Predef.println('Foo', [scala.Any]);
                    }),
                    [java.lang.String], (function(foo) {
                        var $self = this;
                        $self.$init$(java.lang.String.length(foo), [scala.Int]);
                    }),
                    [scala.Boolean], (function(foo) {
                        var $self = this;
                        $self.$init$((function() { if (foo) { return 'true'; } else { return 'false'; } })(), [java.lang.String]);
                    }));
                D.foo = swat.method([], (function() { var $self = this; return $self.$fields.foo; }));
                D = swat.constructor([D, java.lang.Object, scala.Any]);
                   """
        )
    }

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

                def test() {
                    val t: T = null
                    t.x
                    t.y
                    t.y = 456
                    t.z

                    val c = new C
                    c.x
                    c.y
                    c.y = 456
                    c.z

                    O.x
                    O.y
                    O.y = 456
                    O.z
                }
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
                T.x = swat.method([], (function() { var $self = this; return $self.$fields.x; }));
                T.y = swat.method([], (function() { var $self = this; return $self.$fields.y; }));
                T.y_$eq = swat.method([scala.Int], (function(x$1) { var $self = this; $self.$fields.y = x$1; }));
                T.z = swat.method([], (function() { var $self = this; return $self.$fields.z(); }));
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
                C.x = swat.method([], (function() { var $self = this; return $self.$fields.x; }));
                C.y = swat.method([], (function() { var $self = this; return $self.$fields.y; }));
                C.y_$eq = swat.method([scala.Int], (function(x$1) { var $self = this; $self.$fields.y = x$1; }));
                C.z = swat.method([], (function() { var $self = this; return $self.$fields.z(); }));
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
                O$.test = swat.method([], (function() {
                    var $self = this;

                    var t = null;
                    t.x();
                    t.y();
                    t.y_$eq(456, [scala.Int]);
                    t.z();

                    var c = new C();
                    c.x();
                    c.y();
                    c.y_$eq(456, [scala.Int]);
                    c.z();

                    O.x();
                    O.y();
                    O.y_$eq(456, [scala.Int]);
                    O.z();
                }));
                O$.x = swat.method([], (function() { var $self = this; return $self.$fields.x; }));
                O$.y = swat.method([], (function() { var $self = this; return $self.$fields.y; }));
                O$.y_$eq = swat.method([scala.Int], (function(x$1) { var $self = this; $self.$fields.y = x$1; }));
                O$.z = swat.method([], (function() { var $self = this; return $self.$fields.z(); }));
                O = swat.object([O$, java.lang.Object, scala.Any]);
                    """
        )
    }

    test("Methods") {
        """
            class C {
                val x = 123

                def a() { }
                def b(i: Int, j: Int) { }
                def c(i: Int, js: Int*) { }
                def d(i: Int = x, j: Int = x + x) { }

                def test() {
                    a()
                    b(1, 2)
                    b(i = 1, j = 2)
                    b(j = 2, i = 1)
                    c(1)
                    c(1, 2)
                    c(1, 2, 3)
                    c(1, List(2, 3): _*)
                    d()
                    d(1)
                    d(i = 1)
                    d(j = 2)
                    d(1, 2)
                }
            }
        """ shouldCompileTo Map(
            "C" -> """
                swat.provide('C');
                C.$init$ = (function() {
                    var $self = this;
                    $super.$init$.call($self);
                    $self.$fields.x = 123;
                });
                C.a = swat.method([], (function() {
                    var $self = this;
                }));
                C.b = swat.method([scala.Int, scala.Int], (function(i, j) {
                    var $self = this;
                }));
                C.c = swat.method([scala.Int, scala.collection.Seq], (function(i, js) {
                    var $self = this;
                }));
                C.d = swat.method([scala.Int, scala.Int], (function(i, j) {
                    var $self = this;
                }));
                C.d$default$1 = swat.method([], (function() { var $self = this; return $self.x(); }));
                C.d$default$2 = swat.method([], (function() { var $self = this; return ($self.x() + $self.x()); }));

                C.test = swat.method([], (function() {
                    var $self = this;
                    $self.a();
                    $self.b(1, 2, [scala.Int, scala.Int]);
                    $self.b(1, 2, [scala.Int, scala.Int]);
                    (function() {
                        var x$1 = 2;
                        var x$2 = 1;
                        $self.b(x$2, x$1, [scala.Int, scala.Int]);
                    })();
                    $self.c(1, scala.Predef.wrapIntArray([], [scala.Array]), [scala.Int, scala.collection.Seq]);
                    $self.c(1, scala.Predef.wrapIntArray([2], [scala.Array]), [scala.Int, scala.collection.Seq]);
                    $self.c(1, scala.Predef.wrapIntArray([2, 3], [scala.Array]), [scala.Int, scala.collection.Seq]);
                    $self.c(1, scala.collection.immutable.List.apply(scala.Predef.wrapIntArray([2, 3], [scala.Array]), scala.Int, [scala.collection.Seq]), [scala.Int, scala.collection.Seq]);
                    $self.d($self.d$default$1(), $self.d$default$2(), [scala.Int, scala.Int]);
                    $self.d(1, $self.d$default$2(), [scala.Int, scala.Int]);
                    $self.d(1, $self.d$default$2(), [scala.Int, scala.Int]);
                    (function() {
                        var x$3 = 2;
                        var x$4 = $self.d$default$1();
                        $self.d(x$4, x$3, [scala.Int, scala.Int]);
                    })();
                    $self.d(1, 2, [scala.Int, scala.Int]); }));

                    C.x = swat.method([], (function() { var $self = this; return $self.$fields.x; }));
                    C = swat.constructor([C, java.lang.Object, scala.Any]);
            """
        )
    }
}

