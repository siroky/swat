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
            "A" ->
                """
                    swat.provide('A');
                    swat.require('java.lang.Object', true);
                    swat.require('scala.Any', true);
                    swat.require('scala.Predef$', false);

                    A.$init$ = (function() {
                        var $self = this;
                        swat.invokeSuper($self, '$init$', [], 'A');
                        scala.Predef$().println('Foo', 'scala.Any');
                    });
                    A = swat.type('A', [A, java.lang.Object, scala.Any]);
                """,
            "B" ->
                """
                    swat.provide('B');
                    swat.require('java.lang.Object', true);
                    swat.require('scala.Any', true);

                    B.$init$ = (function(foo, bar) {
                        var $self = this;
                        swat.invokeSuper($self, '$init$', [], 'B');
                        $self.$fields.foo = foo;
                        $self.$fields.bar = bar;
                        $self.$fields.baz = true;
                    });
                    B.bar = swat.method('B.bar', '', (function() { var $self = this; return $self.$fields.bar; }));
                    B.baz = swat.method('B.baz', '', (function() { var $self = this; return $self.$fields.baz; }));
                    B.foo = swat.method('B.foo', '', (function() { var $self = this; return $self.$fields.foo; }));
                    B = swat.type('B', [B, java.lang.Object, scala.Any]);
                """,
            "C" ->
                """
                    swat.provide('C');
                    swat.require('java.lang.Object', true);
                    swat.require('scala.Any', true);

                    C.$init$ = (function(foo, bar) {
                        var $self = this;
                        swat.invokeSuper($self, '$init$', [], 'C');
                        swat.setParameter($self, 'foo', foo, 'C');
                        swat.setParameter($self, 'bar', bar, 'C');
                    });
                    C.baz = swat.method('C.baz', '', (function() { var $self = this; return swat.getParameter($self, 'foo', 'C'); }));
                    C = swat.type('C', [C, java.lang.Object, scala.Any]);
                """,
            "D" ->
                """
                    swat.provide('D');
                    swat.require('java.lang.Object', true);
                    swat.require('java.lang.String$', false);
                    swat.require('scala.Any', true);
                    swat.require('scala.Predef$', false);

                    D.$init$ = swat.method('D.$init$', 'scala.Int',
                        (function(foo) {
                            var $self = this;
                            swat.invokeSuper($self, '$init$', [], 'D');
                            $self.$fields.foo = foo;
                        }),
                        '', (function() {
                            var $self = this;
                            swat.invokeThis($self, '$init$', [0, 'scala.Int'], 'D');
                            scala.Predef$().println('Foo', 'scala.Any');
                        }),
                        'java.lang.String', (function(foo) {
                            var $self = this;
                            swat.invokeThis($self, '$init$', [java.lang.String$().length(foo), 'scala.Int'], 'D');
                        }),
                        'scala.Boolean', (function(foo) {
                            var $self = this;
                            swat.invokeThis($self, '$init$', [(function() { if (foo) { return 'true'; } else { return 'false'; } })(), 'java.lang.String'], 'D');
                        }));
                    D.foo = swat.method('D.foo', '', (function() { var $self = this; return $self.$fields.foo; }));
                    D = swat.type('D', [D, java.lang.Object, scala.Any]);
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
            "T" ->
                """
                    swat.provide('T');
                    swat.require('java.lang.Object', true);
                    swat.require('java.lang.String$', false);
                    swat.require('scala.Any', true);

                    T.$init$ = (function() {
                        var $self = this;
                        swat.invokeSuper($self, '$init$', [], 'T');
                        $self.$fields.x = 'abc';
                        $self.$fields.y = 123;
                        $self.$fields.z = swat.lazify((function() {
                            return (java.lang.String$().length($self.x()) + $self.y());
                        }));
                    });
                    T.x = swat.method('T.x', '', (function() { var $self = this; return $self.$fields.x; }));
                    T.y = swat.method('T.y', '', (function() { var $self = this; return $self.$fields.y; }));
                    T.y_$eq = swat.method('T.y_$eq', 'scala.Int', (function(x$1) { var $self = this; $self.$fields.y = x$1; }));
                    T.z = swat.method('T.z', '', (function() { var $self = this; return $self.$fields.z(); }));
                    T = swat.type('T', [T, java.lang.Object, scala.Any]);
                """,
            "C" ->
                """
                    swat.provide('C');
                    swat.require('java.lang.Object', true);
                    swat.require('java.lang.String$', false);
                    swat.require('scala.Any', true);

                    C.$init$ = (function() {
                        var $self = this;
                        swat.invokeSuper($self, '$init$', [], 'C');
                        $self.$fields.x = 'abc';
                        $self.$fields.y = 123;
                        $self.$fields.z = swat.lazify((function() {
                            return (java.lang.String$().length($self.x()) + $self.y());
                        }));
                    });
                    C.x = swat.method('C.x', '', (function() { var $self = this; return $self.$fields.x; }));
                    C.y = swat.method('C.y', '', (function() { var $self = this; return $self.$fields.y; }));
                    C.y_$eq = swat.method('C.y_$eq', 'scala.Int', (function(x$1) { var $self = this; $self.$fields.y = x$1; }));
                    C.z = swat.method('C.z', '', (function() { var $self = this; return $self.$fields.z(); }));
                    C = swat.type('C', [C, java.lang.Object, scala.Any]);
                """,
            "O$" ->
                """
                    swat.provide('O$');
                    swat.require('C', false);
                    swat.require('java.lang.Object', true);
                    swat.require('java.lang.String$', false);
                    swat.require('scala.Any', true);

                    O$.$init$ = (function() {
                        var $self = this;
                        swat.invokeSuper($self, '$init$', [], 'O$');
                        $self.$fields.x = 'abc';
                        $self.$fields.y = 123;
                        $self.$fields.z =
                        swat.lazify((function() {
                            return (java.lang.String$().length($self.x()) + $self.y());
                        }));
                    });
                    O$.test = swat.method('O$.test', '', (function() {
                        var $self = this;

                        var t = null;
                        t.x();
                        t.y();
                        t.y_$eq(456, 'scala.Int');
                        t.z();

                        var c = new C();
                        c.x();
                        c.y();
                        c.y_$eq(456, 'scala.Int');
                        c.z();

                        O$().x();
                        O$().y();
                        O$().y_$eq(456, 'scala.Int');
                        O$().z();
                    }));
                    O$.x = swat.method('O$.x', '', (function() { var $self = this; return $self.$fields.x; }));
                    O$.y = swat.method('O$.y', '', (function() { var $self = this; return $self.$fields.y; }));
                    O$.y_$eq = swat.method('O$.y_$eq', 'scala.Int', (function(x$1) { var $self = this; $self.$fields.y = x$1; }));
                    O$.z = swat.method('O$.z', '', (function() { var $self = this; return $self.$fields.z(); }));
                    O$ = swat.object('O$', [O$, java.lang.Object, scala.Any]);
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
            "C" ->
                """
                    swat.provide('C');
                    swat.require('java.lang.Object', true);
                    swat.require('scala.Any', true);
                    swat.require('scala.Int', false);
                    swat.require('scala.Predef$', false);
                    swat.require('scala.collection.immutable.List$', false);

                    C.$init$ = (function() {
                        var $self = this;
                        swat.invokeSuper($self, '$init$', [], 'C');
                        $self.$fields.x = 123;
                    });
                    C.a = swat.method('C.a', '', (function() {
                        var $self = this;
                    }));
                    C.b = swat.method('C.b', 'scala.Int, scala.Int', (function(i, j) {
                        var $self = this;
                    }));
                    C.c = swat.method('C.c', 'scala.Int, scala.collection.Seq', (function(i, js) {
                        var $self = this;
                    }));
                    C.d = swat.method('C.d', 'scala.Int, scala.Int', (function(i, j) {
                        var $self = this;
                    }));
                    C.d$default$1 = swat.method('C.d$default$1', '', (function() { var $self = this; return $self.x(); }));
                    C.d$default$2 = swat.method('C.d$default$2', '', (function() { var $self = this; return ($self.x() + $self.x()); }));

                    C.test = swat.method('C.test', '', (function() {
                        var $self = this;
                        $self.a();
                        $self.b(1, 2, 'scala.Int, scala.Int');
                        $self.b(1, 2, 'scala.Int, scala.Int');
                        (function() {
                            var x$1 = 2;
                            var x$2 = 1;
                            $self.b(x$2, x$1, 'scala.Int, scala.Int');
                        })();
                        $self.c(1, scala.Predef$().wrapIntArray([], 'scala.Array'), 'scala.Int, scala.collection.Seq');
                        $self.c(1, scala.Predef$().wrapIntArray([2], 'scala.Array'), 'scala.Int, scala.collection.Seq');
                        $self.c(1, scala.Predef$().wrapIntArray([2, 3], 'scala.Array'), 'scala.Int, scala.collection.Seq');
                        $self.c(1, scala.collection.immutable.List$().apply(scala.Predef$().wrapIntArray([2, 3], 'scala.Array'), scala.Int, 'scala.collection.Seq'), 'scala.Int, scala.collection.Seq');
                        $self.d($self.d$default$1(), $self.d$default$2(), 'scala.Int, scala.Int');
                        $self.d(1, $self.d$default$2(), 'scala.Int, scala.Int');
                        $self.d(1, $self.d$default$2(), 'scala.Int, scala.Int');
                        (function() {
                            var x$3 = 2;
                            var x$4 = $self.d$default$1();
                            $self.d(x$4, x$3, 'scala.Int, scala.Int');
                        })();
                        $self.d(1, 2, 'scala.Int, scala.Int');
                    }));

                    C.x = swat.method('C.x', '', (function() { var $self = this; return $self.$fields.x; }));
                    C = swat.type('C', [C, java.lang.Object, scala.Any]);
                """
        )
    }
}

