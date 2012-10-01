package swat.compiler

import frontend.{ClassArtifact, ArtifactRef}

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
        """ shouldCompileTo Map(ArtifactRef(ClassArtifact, "A") -> "A = function() { this.a = 'foo'; };\n")
    }
}
