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
        settings.outdir.value = classTarget
        settings.classpath.value = classPath
        settings.deprecation.value = true
        settings.unchecked.value = true
        settings.feature.value = true

        val compiler = new SwatGlobal(settings)
        val run = new compiler.Run()
        run.compile(List(sourceFile.path))

        compiler.swatCompilerPlugin.output
    }

    private class SwatGlobal(settings: Settings) extends Global(settings, new ExceptionReporter)
    {
        val swatCompilerPlugin = new SwatCompilerPlugin(this)

        override protected def computeInternalPhases() {
            super.computeInternalPhases()
            swatCompilerPlugin.processOptions(options.toList, identity _)
            swatCompilerPlugin.components.foreach(phasesSet += _)
        }
    }
}
