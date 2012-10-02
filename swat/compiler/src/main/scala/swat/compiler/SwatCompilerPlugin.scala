package swat.compiler

import frontend.ScalaAstProcessor
import scala.tools.nsc.Global
import tools.nsc.plugins.{PluginComponent, Plugin}
import reflect.internal.Phase

class SwatCompilerPlugin(val global: Global)
    extends Plugin
    with ScalaAstProcessor
{
    import global._

    val name = "swat-compiler"

    val description = "Swat Compiler of Scala code into JavaScript."

    val components = List[PluginComponent](SwatCompilationComponent)

    private var options = CompilerOptions.default

    private var definitionOutputs = Map.empty[String, js.Program]

    override val optionsHelp = Some(CompilerOptions.help(name))

    override def processOptions(o: List[String], error: String => Unit) {
        super.processOptions(o, error)
        options = CompilerOptions(o)
    }

    def outputs = definitionOutputs

    private object SwatCompilationComponent extends PluginComponent
    {
        val global: SwatCompilerPlugin.this.global.type = SwatCompilerPlugin.this.global

        val runsAfter = List("uncurry")

        val phaseName = "swat-compilation"

        def newPhase(prev: Phase) = new StdPhase(prev) {
            def apply(unit: CompilationUnit) {
                definitionOutputs = processCompilationUnit(unit)
            }
        }
    }
}
