package swat.compiler

@swat.api.dependency(classOf[String])
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

    test("Native classes aren't compiled and get replaced with the native code.") {
        """
            @swat.api.native("A = function() { this.a = 'foo'; };")
            class A
        """ shouldCompileTo Map("A" -> "A = function() { this.a = 'foo'; };\n")
    }

    test("Dependencies with native annotations are supported") {
        """
            @swat.api.native("A = function() { };")
            @swat.api.dependency(classOf[Boolean], false)
            @swat.api.dependency(classOf[String], true)
            class A
        """ shouldCompileTo Map("A" -> """
            swat.dependsOn('scala.Boolean', false);
            swat.dependsOn('java.lang.String', true);
            A = function() { };
        """)
    }
}
