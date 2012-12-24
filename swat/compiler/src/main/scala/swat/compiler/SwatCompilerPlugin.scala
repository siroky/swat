package swat.compiler

import frontend.ScalaAstProcessor
import scala.tools.nsc.Global
import tools.nsc.plugins.{PluginComponent, Plugin}
import scala.reflect.internal.{FatalError, Phase}

class SwatCompilerPlugin(val global: Global)
    extends Plugin
    with ScalaAstProcessor
{
    import global._

    val name = "swat-compiler"

    val description = "Swat Compiler of Scala code into JavaScript."

    val components = List[PluginComponent](SwatCompilationComponent)

    private var options = CompilerOptions.default

    private var classOutputs: Option[Map[String, js.Program]] = None

    override val optionsHelp = Some(CompilerOptions.help(name))

    override def processOptions(o: List[String], error: String => Unit) {
        super.processOptions(o, error)
        options = CompilerOptions(o)
    }

    def outputs = classOutputs

    private object SwatCompilationComponent extends PluginComponent
    {
        val global: SwatCompilerPlugin.this.global.type = SwatCompilerPlugin.this.global

        val runsAfter = List("uncurry")

        val phaseName = "swat-compilation"

        def newPhase(prev: Phase) = new StdPhase(prev) {
            def apply(unit: CompilationUnit) {
                // The compiler plugin shouldn't throw any exceptions. The errors should be reported using the
                // Global.error function. If an exception is thrown out of the plugin, the compiler pollutes the output
                // with long dump of the compilation unit AST, even though the exception may have been caused by a bug
                // in the compiler plugin. To avoid that, all exceptions are consumed here and reported as an internal
                // error of the SWAT compiler.
                try {
                    val transformer = new explicitOuter.ExplicitOuterTransformer(unit)
                    transformer.transformUnit(unit)
                    classOutputs = Some(processCompilationUnit(unit))
                } catch {
                    case f: FatalError => swatCompilerError(f.msg.lines.toBuffer.last, f.getStackTrace)
                    case t: Throwable => swatCompilerError(t.toString, t.getStackTrace)
                }
            }

            def swatCompilerError(msg: String, stackTrace: Seq[StackTraceElement]) {
                println(s"[swat compiler error]: $msg")
                println(stackTrace.mkString("\n"))
            }
        }
    }
}
