package swat.compiler

import scala.tools.nsc.{Global, Settings}
import scala.tools.nsc.io.File
import java.io

class SwatCompiler(
    val classPath: String,
    val classTarget: String,
    val options: SwatCompilerOptions)
{
    def compile(scalaCode: String): js.Program = {
        val sourceFile = new File(new io.File(java.util.UUID.randomUUID + ".scala"))
        try {
            sourceFile.writeAll(scalaCode)
            compile(sourceFile)
        } finally {
            sourceFile.delete()
        }
    }

    def compile(sourceFile: File): js.Program = {
        val settings = new Settings()
        settings.classpath.value = classPath
        settings.outdir.value = classTarget

        val compiler = new InternalCompiler(settings, options.toList)
        val run = new compiler.Run()
        run.compile(List(sourceFile.path))

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
