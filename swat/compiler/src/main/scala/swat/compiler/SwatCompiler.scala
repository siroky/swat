package swat.compiler

import scala.tools.nsc.{Global, Settings}
import scala.tools.nsc.io.File
import java.io
import tools.nsc.reporters.Reporter
import reflect.internal.util.Position
import collection.mutable

class SwatCompiler(
    val classPath: String,
    val classTarget: String,
    val options: CompilerOptions) {

    def compile(scalaCode: String): CompilationOutput = {
        val uuid = java.util.UUID.randomUUID
        val sourceFile = new File(new io.File(s"$uuid.scala"))
        try {
            sourceFile.writeAll(scalaCode)
            compile(sourceFile)
        } finally {
            sourceFile.delete()
        }
    }

    def compile(sourceFile: File): CompilationOutput = {
        val settings = new Settings()
        settings.outdir.value = classTarget
        settings.classpath.value = classPath
        settings.deprecation.value = true
        settings.unchecked.value = true
        settings.feature.value = true
        settings.stopBefore.value = List("erasure")

        val reporter = new SilentReporter
        val compiler = new SwatGlobal(settings, reporter)
        val run = new compiler.Run()
        try {
            run.compile(List(sourceFile.path))
        } catch {
            case t: Throwable if compiler.swatPlugin.output.isEmpty => throw t
            case _: Throwable =>
        }

        val classOutputs = compiler.swatPlugin.output
        if (reporter.errors.nonEmpty && classOutputs.isEmpty) {
            throw new CompilationException(reporter.errors.head)
        }
        CompilationOutput(classOutputs.getOrElse(Map.empty), reporter.warnings.toList, reporter.infos.toList)
    }

    private class SwatGlobal(settings: Settings, reporter: Reporter) extends Global(settings, reporter) {

        val swatPlugin = new SwatCompilerPlugin(this)

        override protected def computeInternalPhases() {
            super.computeInternalPhases()
            swatPlugin.processOptions(options.toList, identity _)
            swatPlugin.components.foreach(phasesSet += _)
        }
    }

    private class SilentReporter extends Reporter {

        val errors = mutable.ListBuffer.empty[String]
        val warnings = mutable.ListBuffer.empty[String]
        val infos = mutable.ListBuffer.empty[String]

        protected def info0(pos: Position, msg: String, severity: Severity, force: Boolean) {
            val (messages, severityDescription) = severity match {
                case ERROR => (errors, "error")
                case WARNING => (warnings, "warning")
                case INFO => (infos, "info")
            }
            val positionDescription =
                if (pos.isDefined) "\nOn line %s column %s: %s".format(pos.line, pos.column, pos.lineContent) else ""

            messages += s"[$severityDescription] $msg $positionDescription"
        }
    }
}
