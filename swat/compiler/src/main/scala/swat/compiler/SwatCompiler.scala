package swat.compiler

import scala.tools.nsc.{SubComponent, Global, Settings}
import scala.tools.nsc.io.File
import java.io
import tools.nsc.reporters.Reporter
import reflect.internal.util.Position
import collection.mutable
import scala.tools.nsc.transform.Erasure

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

        val reporter = new SilentReporter
        val compiler = new SwatGlobal(settings, reporter)
        val run = new compiler.Run()
        run.compile(List(sourceFile.path))

        val classOutputs = compiler.swatPlugin.output
        if (reporter.errors.nonEmpty && classOutputs.isEmpty) {
            throw new CompilationException(reporter.errors.head)
        }
        CompilationOutput(classOutputs.getOrElse(Map.empty), reporter.warnings.toList, reporter.infos.toList)
    }

    private class SwatGlobal(settings: Settings, reporter: Reporter) extends Global(settings, reporter) {

        object swatPlugin extends SwatCompilerPlugin(this)

        object postponedErasure extends {
            val global: SwatGlobal.this.type = SwatGlobal.this
            val runsAfter = List("swat")
            val runsRightAfter = Some("swat")
        } with Erasure

        override protected def computeInternalPhases() {
            super.computeInternalPhases()
            swatPlugin.processOptions(options.toList, identity _)

            // Add the swat compiler phases.
            swatPlugin.components.foreach(c => addToPhasesSet(c, c.phaseName))

            // Remove the old erasure that runs right after explicitouter and add new that runs after swat.
            removeFromPhasesSet(erasure)
            removeFromPhasesSet(specializeTypes) // TODO investigate
            addToPhasesSet(postponedErasure, "erase types, add interfaces for traits")
        }

        protected def removeFromPhasesSet(sub: SubComponent) {
            phasesSet -= sub
            phasesDescMap -= sub
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
