package swat.compiler

import frontend.{ClassArtifact, ArtifactRef}
import org.scalatest.FunSuite
import tools.nsc.io.Directory
import swat.compiler.backend.JsCodeGenerator
import swat.compiler.js._

trait CompilerSuite extends FunSuite with JsCodeGenerator
{
    implicit protected def string2scalaCode(code: String)= new ScalaCode(code)

    implicit protected def string2scalaCodeFragment(code: String) = new ScalaCodeFragment(code)

    protected class ScalaCode(val code: String)
    {
        def shouldCompileTo(expectedCodes: Map[ArtifactRef, String]) {
            shouldCompileTo(expectedCodes, jsAstToCode _)
        }

        def shouldCompileToPrograms(expectedPrograms: Map[ArtifactRef, js.Program]) {
            shouldCompileTo(expectedPrograms, identity _)
        }

        protected def shouldCompileTo[A](expectedOutputs: Map[ArtifactRef, A], astProcessor: js.Ast => A) {
            val compilationOutput = compile()
            val actualOutputs = compilationOutput.artifactOutputs.mapValues(astProcessor)
            val e = expectedOutputs.toSet
            val a = actualOutputs.toSet
            val difference = (a diff e) union (e diff a)

            difference.headOption.foreach { case (ref, _) =>
                expectationFail(ref.fullName, expectedOutputs.get(ref), actualOutputs.get(ref))
            }

            val additionalInfos = compilationOutput.warnings ++ compilationOutput.infos
            if (additionalInfos.nonEmpty) {
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

        private def expectationFail(classFullName: String, expected: Any, actual: Any) {
            fail(
                """|The compiler output of class %s doesn't correspond to the expected result.
                   |    EXPECTED: %s
                   |    ACTUAL:   %s
                """.stripMargin.format(classFullName, expected, actual))
        }
    }

    protected class ScalaCodeFragment(code: String)
    {
        private val ref = ArtifactRef(ClassArtifact, "A")

        private val scalaCode = new ScalaCode("class A { def f() { %s } }".format(code)) {
            override def compile(): CompilationOutput = {
                val output = super.compile()
                val functionBody = output.artifactOutputs.get(ref).flatMap {
                    _.elements.collect {
                        case AssignmentStatement(MemberExpression(_, Identifier("f")), f: FunctionDeclaration) => {
                            f.body
                        }
                    }.headOption
                }

                CompilationOutput(Map(ref -> Program(functionBody.toList.flatten)), output.warnings, output.infos)
            }
        }

        def fragmentShouldCompileTo(expectedCode: String) {
            scalaCode.shouldCompileTo(Map(ref -> expectedCode))
        }

        def fragmentShouldCompileTo(expectedProgram: js.Program) {
            scalaCode.shouldCompileToPrograms(Map(ref -> expectedProgram))
        }
    }
}
