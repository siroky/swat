package swat.compiler

class ClassDefinitionTests extends CompilerSuite
{
    test("Adapter classes and ignored classes aren't compiled") {
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
    }

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
                    $self.$super().$init$();
                });
                A = swat.constructor([A, java.lang.Object, scala.Any]);
            """,

            "$colon$colon$less$greater" -> """
                swat.provide('$colon$colon$less$greater');
                $colon$colon$less$greater.$init$ = (function() {
                    var $self = this;
                    $self.$super().$init$();
                });
                $colon$colon$less$greater = swat.constructor([$colon$colon$less$greater, java.lang.Object, scala.Any]);
            """,

            "foo.B" -> """
                swat.provide('foo.B');
                foo.B.$init$ = (function() {
                    var $self = this;
                    $self.$super().$init$();
                });
                foo.B = swat.constructor([foo.B, java.lang.Object, scala.Any]);
            """,

            "foo.bar$" -> """
                swat.provide('foo.bar$');
                foo.bar$.$init$ = (function() {
                    var $self = this;
                    $self.$super().$init$();
                });
                foo.bar = swat.object(swat.constructor([foo.bar$, java.lang.Object, scala.Any]));
            """,

            "foo.bar.baz.C" -> """
                swat.provide('foo.bar.baz.C');
                foo.bar.baz.C.$init$ = (function() {
                    var $self = this;
                    $self.$super().$init$();
                });
                foo.bar.baz.C = swat.constructor([foo.bar.baz.C, java.lang.Object, scala.Any]);
            """,

            "foo.bar.baz.C$D" -> """
                swat.provide('foo.bar.baz.C$D');
                foo.bar.baz.C$D.$init$ = (function() {
                    var $self = this;
                    $self.$super().$init$();
                });
                foo.bar.baz.C$D = swat.constructor([foo.bar.baz.C$D, java.lang.Object, scala.Any]);
            """,

            "foo.bar.baz.C$E" -> """
                swat.provide('foo.bar.baz.C$E');
                foo.bar.baz.C$E = swat.constructor([foo.bar.baz.C$E, java.lang.Object, scala.Any]);
            """
        )
    }
}
