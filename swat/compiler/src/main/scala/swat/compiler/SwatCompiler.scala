package swat.compiler

import scala.tools.nsc.{Global, Settings}

class SwatCompiler(
    val classPath: String,
    val classTarget: String,
    val options: SwatCompilerOptions)
{
    def compile(sourceFileName: String): Option[js.Program] = {
        val settings = new Settings()
        settings.classpath.value = classPath
        settings.outdir.value = classTarget

        val compiler = new InternalCompiler(settings, options.toList)
        val run = new compiler.Run()
        run.compile(List(sourceFileName))

        compiler.output
    }

    private class InternalCompiler(settings: Settings, val options: List[String]) extends Global(settings)
    {
        private val swatCompilerPlugin = new SwatCompilerPlugin(this)

        def output = swatCompilerPlugin.compilationComponent.output

        override protected def computeInternalPhases() {
            super.computeInternalPhases()
            swatCompilerPlugin.processOptions(options, identity _)
            swatCompilerPlugin.components.foreach(phasesSet += _)
        }
    }
}
