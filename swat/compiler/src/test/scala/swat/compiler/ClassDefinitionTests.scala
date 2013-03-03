package swat.compiler

class ClassDefinitionTests extends CompilerSuite
{
    /*test("Adapter classes and ignored classes aren't compiled") {
        """
            import swat.api._

            @ignored class A1
            @ignored trait T1
            @ignored object O1

            @adapter class A2
            @adapter trait T2
            @adapter object O2
        """ shouldCompileToPrograms Map.empty
    }

    test("Native classes aren't compiled and get replaced with the native code") {
        """
            @swat.api.native("A = function() { this.a = 'foo'; };")
            class A
        """ shouldCompileTo Map("A" -> """
            swat.provide('A');
            A = function() { this.a = 'foo'; };
        """)
    }

    test("Dependencies with native annotations are supported") {
        """
            import swat.api._

            @native("A = function() { };")
            @dependency(classOf[Boolean], false)
            @dependency(classOf[String], true)
            class A
        """ shouldCompileTo Map("A" -> """
            swat.provide('A');
            swat.require('scala.Boolean', false);
            swat.require('java.lang.String', true);
            A = function() { };
        """)
    }*/

    test("Definitions are properly qualified with respect to packages and outer classes") {
        """
            import swat.api._

            class A

            class ::<>

            package foo
            {
                class B

                package object bar

                package bar.baz
                {
                    class C
                    {
                        class D
                        trait E
                    }
                }
            }
        """ shouldCompileTo Map(
            "A" -> """
                swat.provide('A');
                A.$init$ = (function() {
                    var $self = this;
                    $super.$init$.call($self);
                });
                A = swat.type([A, java.lang.Object, scala.Any]);
            """,

            "$colon$colon$less$greater" -> """
                swat.provide('$colon$colon$less$greater');
                $colon$colon$less$greater.$init$ = (function() {
                    var $self = this;
                    $super.$init$.call($self);
                });
                $colon$colon$less$greater = swat.type([$colon$colon$less$greater, java.lang.Object, scala.Any]);
            """,

            "foo.B" -> """
                swat.provide('foo.B');
                foo.B.$init$ = (function() {
                    var $self = this;
                    $super.$init$.call($self);
                });
                foo.B = swat.type([foo.B, java.lang.Object, scala.Any]);
            """,

            "foo.bar$" -> """
                swat.provide('foo.bar$');
                foo.bar$.$init$ = (function() {
                    var $self = this;
                    $super.$init$.call($self);
                });
                foo.bar$ = swat.object([foo.bar$, java.lang.Object, scala.Any]);
            """,

            "foo.bar.baz.C" -> """
                swat.provide('foo.bar.baz.C');
                foo.bar.baz.C.$init$ = (function() {
                    var $self = this;
                    $super.$init$.call($self);
                });
                foo.bar.baz.C = swat.type([foo.bar.baz.C, java.lang.Object, scala.Any]);
            """,

            "foo.bar.baz.C$D" -> """
                swat.provide('foo.bar.baz.C$D');
                foo.bar.baz.C$D.$init$ = (function($outer) {
                    var $self = this;
                    $super.$init$.call($self);
                    $self.$outer = $outer;
                });
                foo.bar.baz.C$D = swat.type([foo.bar.baz.C$D, java.lang.Object, scala.Any]);
                                 """,

            "foo.bar.baz.C$E" -> """
                swat.provide('foo.bar.baz.C$E');
                foo.bar.baz.C$E = swat.type([foo.bar.baz.C$E, java.lang.Object, scala.Any]);
            """
        )
    }

    /*test("Inner classes") {
        """
            class A {
                def a = new A
                def b = new B
                def c = new C

                class B {
                    def a = new A
                    def b = new B
                    def c = new C
                }

                class C
            }

            object o {
                val a = new A
                val b = new a.B
                val c = new a.C

                def x() {
                    val a = new A
                    new a.B
                    new o.a.B
                }
            }
        """ shouldCompileTo Map(
            "A" -> """
                swat.provide('A');

                A.$init$ = (function() { var $self = this; $super.$init$.call($self); });
                A.a = swat.method([], (function() { var $self = this; return new A(); }));
                A.b = swat.method([], (function() { var $self = this; return new A$B($self); }));
                A.c = swat.method([], (function() { var $self = this; return new A$C($self); }));
                A = swat.type([A, java.lang.Object, scala.Any]);
            """,

            "A$B" -> """
                swat.provide('A$B');

                A$B.$init$ = (function($outer) {
                    var $self = this;
                    $super.$init$.call($self);
                    $self.$outer = $outer;
                });
                A$B.a = swat.method([], (function() { var $self = this; return new A(); }));
                A$B.b = swat.method([], (function() { var $self = this; return new A$B($self.$outer); }));
                A$B.c = swat.method([], (function() { var $self = this; return new A$C($self.$outer); }));
                A$B = swat.type([A$B, java.lang.Object, scala.Any]);
            """,

            "A$C" -> """
                swat.provide('A$C');

                A$C.$init$ = (function($outer) {
                    var $self = this;
                    $super.$init$.call($self);
                    $self.$outer = $outer;
                });
                A$C = swat.type([A$C, java.lang.Object, scala.Any]);
            """,

            "o$" -> """
                swat.provide('o$');

                o$.$init$ = (function() {
                    var $self = this;
                    $super.$init$.call($self);
                    $self.$fields.a = new A();
                    $self.$fields.b = new A$B($self.a());
                    $self.$fields.c = new A$C($self.a());
                });
                o$.a = swat.method([], (function() { var $self = this; return $self.$fields.a; }));
                o$.b = swat.method([], (function() { var $self = this; return $self.$fields.b; }));
                o$.c = swat.method([], (function() { var $self = this; return $self.$fields.c; }));
                o$.x = swat.method([], (function() {
                    var $self = this;
                    var a = new A();
                    new A$B(a);
                    new A$B(o$().a());
                }));
                o$ = swat.object([o$, java.lang.Object, scala.Any]);
            """
        )
    }*/
}
