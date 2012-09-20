package swat.compiler

class CoreTests extends CompilerSuite
{
    test("compiler suite 1") {
        "class A" shouldCompileTo ""
    }

    /*test("compiler suite 2") {
        "class A" shouldCompileToAst js.Program(Nil)
    }

    test("compiler suite 3") {
        "val x = 1234".asFragment shouldCompileTo ""
    }

    test("compiler suite 4") {
        "val x = 1234".asFragment shouldCompileToAst js.Program(Nil)
    }*/
}
