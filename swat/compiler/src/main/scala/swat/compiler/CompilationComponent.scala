package swat.compiler

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent
import scala.reflect.internal.Phase

class CompilationComponent(val global: Global, val plugin: SwatCompilerPlugin) extends PluginComponent
{
    import global._

    val runsAfter = List[String]("refchecks")

    val phaseName = "swat-compilation"

    private var _output = js.Program()

    def output = _output

    def newPhase(prev: Phase) = new StdPhase(prev) {
        def apply(unit: CompilationUnit) {
            println("Hello from Swat.")
        }
    }
}
