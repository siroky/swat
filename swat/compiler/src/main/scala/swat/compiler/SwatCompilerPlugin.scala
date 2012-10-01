package swat.compiler

import frontend.{ArtifactRef, JsAstGenerator}
import scala.tools.nsc.Global
import tools.nsc.plugins.{PluginComponent, Plugin}
import reflect.internal.Phase

class SwatCompilerPlugin(val global: Global) extends Plugin
{
    import global._

    val name = "swat-compiler"

    val description = "Swat Compiler of Scala code into JavaScript."

    val components = List[PluginComponent](SwatCompilationComponent)

    private var options = CompilerOptions.default

    private var artifactOutputs = Map.empty[ArtifactRef, js.Program]

    override val optionsHelp = Some(CompilerOptions.help(name))

    override def processOptions(o: List[String], error: String => Unit) {
        super.processOptions(o, error)
        options = CompilerOptions(o)
    }

    def outputs = artifactOutputs

    private object SwatCompilationComponent
        extends PluginComponent
        with JsAstGenerator
    {
        val global: SwatCompilerPlugin.this.global.type = SwatCompilerPlugin.this.global

        val runsAfter = List("uncurry")

        val phaseName = "swat-compilation"

        def newPhase(prev: Phase) = new StdPhase(prev) {
            def apply(unit: CompilationUnit) {
                artifactOutputs = processCompilationUnit(unit)
            }
        }
    }
}
