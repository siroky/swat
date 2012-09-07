package swat.compiler

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin

class SwatCompilerPlugin(val global: Global) extends Plugin
{
    val name = "swat-compiler"

    val description = "Swat Compiler of Scala sources into JavaScript."

    val compilationComponent = new CompilationComponent(global, this)

    val components = List(compilationComponent)

    override val optionsHelp = Some(SwatCompilerOptions.help(name))

    private var _options = SwatCompilerOptions.default

    def options = _options

    override def processOptions(options: List[String], error: String => Unit) {
        super.processOptions(options, error)
        _options = SwatCompilerOptions(options)
    }
}
