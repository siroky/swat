package swat.compiler

import org.scalatest.FunSuite
import tools.nsc.io.Directory
import swat.compiler.backend.CodeGenerator
import swat.compiler.js._

trait CompilerSuite extends FunSuite
{
    implicit protected def string2scalaCode(code: String): ScalaCode = new ScalaCode(code)

    protected class ScalaCode(val code: String)
    {
        def asFragment = new ScalaCodeFragment(code)

        def shouldCompileToAst(expectedAst: Ast) {
            shouldCompileTo(p => (p == expectedAst, p, expectedAst))
        }

        def shouldCompileTo(expectedCode: String) {
            shouldCompileTo { p =>
                val actualCode = new CodeGenerator().run(p)
                (actualCode == expectedCode, actualCode, expectedCode)
            }
        }

        private def shouldCompileTo(outputChecker: Program => (Boolean, Any, Any)) {
            val compilationOutput = compile()
            val (success: Boolean, expected, actual) = outputChecker(compilationOutput.program)
            if (!success) {
                compilationFail(expected, actual)
            } else {
                val additionalInfos = compilationOutput.warnings ++ compilationOutput.infos
                if (additionalInfos.nonEmpty) {
                    info(additionalInfos.mkString("\n"))
                }
            }
        }

        protected def compile(): CompilationOutput = {
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
            } catch {
                case ce: CompilationException => {
                    fail(ce.getMessage)
                    null
                }
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
        override def compile(): CompilationOutput = {
            val output = super.compile()
            val functionBody = output.program match {
                case Program(elements) => {
                    elements.collect {
                        case AssignmentStatement(MemberExpression(_, Identifier("f")), f: FunctionDeclaration) => {
                            f.body
                        }
                    }.headOption
                }
                case _ => None
            }

            CompilationOutput(Program(functionBody.getOrElse(Nil)), output.warnings, output.infos)
        }
    }
}
