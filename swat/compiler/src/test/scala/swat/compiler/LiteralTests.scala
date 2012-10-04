package swat.compiler

class LiteralTests extends CompilerSuite
{
    test("Boolean literals are compiled properly.") {
        """
            true
            false
        """ fragmentShouldCompileTo """
            true;
            false;
        """
    }
}
