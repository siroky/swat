package swat.compiler

import backend.JsCodeGenerator
import org.scalatest.FunSuite
import swat.compiler.js._

trait CompilerSuite extends FunSuite {

    implicit protected def string2scalaCode(code: String)= new ScalaCode(code)
    implicit protected def string2scalaCodeFragment(code: String) = new ScalaCodeFragment(code)

    protected class ScalaCode(val code: String) {
        def shouldCompileTo(expectedCodes: Map[String, String]) {
            def normalizeCode(c: String) = {
                c.lines.map(_.dropWhile(_ == ' ').reverse.dropWhile(_ == ' ').reverse).filter(!_.isEmpty).mkString(" ")
            }

            val codeGenerator = new JsCodeGenerator
            shouldCompileTo(expectedCodes.mapValues(normalizeCode _), c => normalizeCode(codeGenerator.astToCode(c)))
        }

        def shouldCompileTo(classIdentifier: String)(code: String) {
            shouldCompileTo(Map(classIdentifier -> code))
        }

        def shouldCompileToPrograms(expectedPrograms: Map[String, js.Program]) {
            shouldCompileTo(expectedPrograms, identity _)
        }

        protected def shouldCompileTo[A](expectedOutputs: Map[String, A], astProcessor: js.Ast => A) {
            val compilationOutput = compile()
            val actualOutputs = compilationOutput.typeOutputs.mapValues(astProcessor)
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
                info(relevantInfos.mkString("\n"))
            }
        }

        protected def compile(): CompilationOutput = {
            try {
                new SwatCompiler(None, None, None).compile(code)
            } catch {
                case ce: CompilationException => {
                    fail(ce.getMessage)
                    null
                }
            }
        }
    }

    protected class ScalaCodeFragment(code: String) {
        private val ident = "A"

        private val scalaCode = new ScalaCode(s"class A { def f() { $code } }") {
            override def compile(): CompilationOutput = {
                val output = super.compile()
                val functionBody = output.typeOutputs.get(ident).flatMap {
                    _.elements.flatMap {
                        case AssignmentStatement(MemberExpression(_, Identifier("f")), rhs) => rhs match {
                            case CallExpression(_, List(_, f: FunctionExpression)) => Some(f.body.tail)
                            case _ => None
                        }
                        case _ => None
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
