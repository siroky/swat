package swat.compiler

import backend.JsCodeGenerator
import org.scalatest.FunSuite
import tools.nsc.io.Directory
import swat.compiler.js._

trait CompilerSuite extends FunSuite
{
    implicit protected def string2scalaCode(code: String)= new ScalaCode(code)

    implicit protected def string2scalaCodeFragment(code: String) = new ScalaCodeFragment(code)

    protected class ScalaCode(val code: String)
    {
        def shouldCompileTo(expectedCodes: Map[String, String]) {
            def normalizeCode(c: String) = {
                c.lines.map(_.dropWhile(_ == ' ').reverse.dropWhile(_ == ' ').reverse).filter(!_.isEmpty).mkString(" ")
            }

            val codeGenerator = new JsCodeGenerator
            shouldCompileTo(expectedCodes.mapValues(normalizeCode _), c => normalizeCode(codeGenerator.astToCode(c)))
        }

        def shouldCompileTo(definitionIdentifier: String)(code: String) {
            shouldCompileTo(Map(definitionIdentifier -> code))
        }

        def shouldCompileToPrograms(expectedPrograms: Map[String, js.Program]) {
            shouldCompileTo(expectedPrograms, identity _)
        }

        protected def shouldCompileTo[A](expectedOutputs: Map[String, A], astProcessor: js.Ast => A) {
            val compilationOutput = compile()
            val actualOutputs = compilationOutput.definitionOutputs.mapValues(astProcessor)
            val e = expectedOutputs.toSet
            val a = actualOutputs.toSet
            val difference = (a diff e) union (e diff a)

            difference.headOption.foreach { case (ident, _) =>
                fail(
                    """|The compiler output of class %s doesn't correspond to the expected result.
                       |    EXPECTED: %s
                       |    ACTUAL:   %s
                       |    FULL OUTPUT: %s
                    """.stripMargin.format(ident, expectedOutputs.get(ident), actualOutputs.get(ident), actualOutputs))
            }

            val additionalInfos = compilationOutput.warnings ++ compilationOutput.infos
            val relevantInfos = additionalInfos.filter(!_.startsWith("[warning] a pure expression does nothing"))
            if (relevantInfos.nonEmpty) {
                info(additionalInfos.mkString("\n"))
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
                new SwatCompiler(classPath, classTarget.path, CompilerOptions(target = None)).compile(code)
            } catch {
                case ce: CompilationException => {
                    fail(ce.getMessage)
                    null
                }
            } finally {
                classTarget.deleteRecursively()
            }
        }
    }

    protected class ScalaCodeFragment(code: String)
    {
        private val ident = "A"

        private val scalaCode = new ScalaCode("class A { def f() { %s } }".format(code)) {
            override def compile(): CompilationOutput = {
                val output = super.compile()
                val functionBody = output.definitionOutputs.get(ident).flatMap {
                    _.elements.collect {
                        case AssignmentStatement(MemberExpression(_, Identifier("f")), f: FunctionExpression) => f.body
                    }.headOption
                }

                CompilationOutput(Map(ident -> Program(functionBody.toList.flatten)), output.warnings, output.infos)
            }
        }

        def fragmentShouldCompileTo(expectedCode: String) {
            scalaCode.shouldCompileTo(Map(ident -> expectedCode))
        }

        def fragmentShouldCompileTo(expectedProgram: js.Program) {
            scalaCode.shouldCompileToPrograms(Map(ident -> expectedProgram))
        }
    }
}
