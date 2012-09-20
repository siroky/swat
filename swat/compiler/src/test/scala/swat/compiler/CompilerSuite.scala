package swat.compiler

import org.scalatest.FunSuite
import swat.compiler.js._
import tools.nsc.io.Directory

trait CompilerSuite extends FunSuite
{
    implicit protected def string2scalaCode(code: String): ScalaCode = new ScalaCode(code)

    protected class ScalaCode(val code: String)
    {
        def asFragment = new ScalaCodeFragment(code)

        def shouldCompileToAst(expectedAst: Ast) {
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

        protected def compileToAst: Ast = {
            // Inherit the compiler class path from the from the current classpath.
            val urls = java.lang.Thread.currentThread.getContextClassLoader match {
                case cl: java.net.URLClassLoader => cl.getURLs.toList
                case _ => fail("Couldn't provide the current classpath to the compiler.")
            }
            val classPath = urls.map(_.getFile).mkString(java.io.File.pathSeparator)

            // Create a temporary class target.
            val classTarget = new Directory(new java.io.File(java.util.UUID.randomUUID.toString))
            classTarget.createDirectory()

            try {
                new SwatCompiler(classPath, classTarget.path, SwatCompilerOptions(target = None)).compile(code)
            } finally {
                classTarget.deleteRecursively()
            }
        }

        private def compilationFail(expected: Any, actual: Any) {
            fail(
                """|The compiler output doesn't correspond to the expected result.
                   |    EXPECTED >>>%s<<<
                   |    ACTUAL   >>>%s<<<
                """.stripMargin.format(expected, actual))
        }
    }

    protected class ScalaCodeFragment(code: String) extends ScalaCode("class A { def f() { %s } }".format(code))
    {
        override def compileToAst: Ast = {
            val functionBody = super.compileToAst match {
                case Program(elements) => {
                    elements.collect {
                        case AssignmentStatement(MemberExpression(_, Identifier("f")), f: FunctionDeclaration) => {
                            f.body
                        }
                    }.headOption
                }
                case _ => None
            }

            Program(functionBody.getOrElse(Nil))
        }
    }
}
