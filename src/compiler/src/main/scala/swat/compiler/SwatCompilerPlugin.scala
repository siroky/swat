package swat.compiler

import frontend.ScalaAstProcessor
import scala.tools.nsc.Global
import tools.nsc.plugins.{PluginComponent, Plugin}
import scala.reflect.internal.{FatalError, Phase}
import scala.collection.mutable
import swat.compiler.backend.JsCodeGenerator

/**
 * A compiler plugin that produces JavaScript that should behave equally to the input Scala AST.
 * @param global The compiler where the plugin is plugged in.
 */
class SwatCompilerPlugin(val global: Global) extends Plugin with ScalaAstProcessor {
    import global._

    val name = "swat-compiler"
    val description = "Swat Compiler of Scala code into JavaScript."
    val components = List[PluginComponent](SwatCompilationComponent)
    var typeOutputs = mutable.ListBuffer[TypeOutput]()
    val backend = new JsCodeGenerator

    /**
     * The only component of the plugin.
     */
    private object SwatCompilationComponent extends PluginComponent {
        val global: SwatCompilerPlugin.this.global.type = SwatCompilerPlugin.this.global
        val phaseName = "swat"
        val runsAfter = List("explicitouter")

        /**
         * Returns a phase that performs the JavaScript compilation.
         * @param prev The previous phase.
         */
        def newPhase(prev: Phase) = new StdPhase(prev) {
            def apply(unit: CompilationUnit) {
                // The compiler plugin shouldn't throw any exceptions. The errors should be reported using the
                // Global.error function. If an exception is thrown out of the plugin, the compiler pollutes the output
                // with long dump of the compilation unit AST, even though the exception may have been caused by a bug
                // in the compiler plugin. To avoid that, all exceptions are consumed here and reported as an internal
                // error of the Swat compiler.
                try {
                    typeOutputs ++= processUnitBody(unit.body)
                } catch {
                    case f: FatalError => swatError(f.msg.lines.toBuffer.last, f.getStackTrace)
                    case t: Throwable => swatError(t.toString, t.getStackTrace)
                }
            }
        }
    }

    /**
     * Reports an error in the SWAT compiler itself.
     * @param msg The error message.
     * @param stackTrace Stack trace of the error.
     */
    def swatError(msg: String, stackTrace: Seq[StackTraceElement]) {
        println(s"[swat error]: $msg")
        println(stackTrace.mkString("\n"))
    }
}

/**
 * A compilation output of one type.
 * @param identifier Identifier of the type.
 * @param code The output JavaScript program.
 * @param program The output JavaScript program represented as an AST.
 */
case class TypeOutput(identifier: String, code: String, program: js.Ast)

/**
 * An output of the swat plugin phase compilation.
 * @param typeOutputs A list of type outputs.
 * @param warnings A list of warnings that occurred during compilation.
 * @param infos A list of infos that occurred during compilation.
 */
case class CompilationOutput(typeOutputs: List[TypeOutput], warnings: List[String], infos: List[String])
