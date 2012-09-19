package swat.compiler

import java.io.File
import js.CodeGenerator
import org.scalatest.FunSuite

trait CompilerSuite extends FunSuite
{
    val classTarget = new File("compiler/target/compiler-test-classes")
    classTarget.mkdirs()

    protected def scalaCode(code: String): ScalaCode = new ScalaCode(code)

    protected def scalaCodeFragment(code: String): ScalaCodeFragment = new ScalaCodeFragment(code)

    protected class ScalaCode(code: String)
    {
        def shouldCompileToAst(expectedAst: js.Ast) {
            val actualAst = compileToAst
            if (expectedAst != actualAst) {
                compilationFail(expectedAst, actualAst)
            }
        }

        def shouldCompileTo(expectedCode: String) {
            val actualCode = new CodeGenerator().run(compileToAst)
            if (expectedCode != actualCode) {
                compilationFail(expectedCode, actualCode)
            }
        }

        protected def compileToAst: js.Ast = {
            new SwatCompiler("/lib", classTarget.getAbsolutePath, SwatCompilerOptions(target = None)).compile(code)
        }

        private def compilationFail(expected: Any, actual: Any) {
            fail(
                """|The compiler output doesn't correspond to the expected result.
                   |    EXPECTED >>>%s<<<
                   |    ACTUAL   >>>%s<<<
                """.stripMargin.format(expected, actual))
        }
    }

    protected class ScalaCodeFragment(code: String) extends ScalaCode(code)
    {

    }
}
