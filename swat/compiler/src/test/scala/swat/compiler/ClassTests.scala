package swat.compiler

class ClassTests extends CompilerSuite
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

    test("Classes are properly qualified with respect to packages and outer classes") {
        """
            import swat.api._

            class A

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
            """,
            "foo.B" -> """
                swat.provide('foo.B');
            """,
            "foo.bar.package$" -> """
                swat.provide('foo.bar.package$');
            """,
            "foo.bar.baz.C" -> """
                swat.provide('foo.bar.baz.C');
            """,
            "foo.bar.baz.C$D" -> """
                swat.provide('foo.bar.baz.C$D');
            """,
            "foo.bar.baz.C$E" -> """
                swat.provide('foo.bar.baz.C$E');
            """
        )
    }
}
