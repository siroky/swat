package swat.compiler

import scala.tools.nsc.{SubComponent, Global, Settings}
import scala.tools.nsc.io.File
import java.io
import tools.nsc.reporters.Reporter
import reflect.internal.util.Position
import collection.mutable
import scala.tools.nsc.transform.Erasure

/**
 * A Scala compiler that, on the top of the standard Scala compiler, includes the [[swat.compiler.SwatCompilerPlugin]]
 * plugin producing JavaScript that should behave equally to the input Scala AST. Implemented as a wrapper of an
 * extended Scala compiler.
 * @param classPath Classpath for the compiled sources.
 * @param classTarget Target directory where the class files are stored.
 * @param javaScriptTarget The target directory where the generated JavaScript files optionally are stored.
 */
class SwatCompiler(val classPath: String, val classTarget: String, val javaScriptTarget: Option[String]) {

    /**
     * Compiles the specified source code.
     * @param scalaCode The Scala code to compile.
     * @return A compilation output.
     */
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

    /**
     * Compiles the specified file.
     * @param sourceFile The Scala source file to compile.
     * @return A compilation output.
     */
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

    /**
     * An internal extension of the standard Scala compiler. In order to ensure that the swat compiler phase runs right
     * after explicitouter but before type erasure, the erasure phase has to be altered to have different dependencies
     * (i.e. to run right after swat, not explicitouter).
     * @param settings Compilation settings.
     * @param reporter A reporter used to report compilation infos.
     */
    private class SwatGlobal(settings: Settings, reporter: Reporter) extends Global(settings, reporter) {

        /** The swat plugin. */
        object swatPlugin extends SwatCompilerPlugin(this)

        /** The erasure component with altered dependencies. */
        object postponedErasure extends {
            val global: SwatGlobal.this.type = SwatGlobal.this
            val runsAfter = List("swat")
            val runsRightAfter = Some("swat")
        } with Erasure

        /**
         * Adds the internal compiler phases to the phases set.
         */
        override protected def computeInternalPhases() {
            super.computeInternalPhases()

            // Alter the compiler phases.
            swatPlugin.components.foreach(c => addToPhasesSet(c, c.phaseName))
            removeFromPhasesSet(erasure)
            removeFromPhasesSet(specializeTypes) // TODO investigate
            addToPhasesSet(postponedErasure, "erase types, add interfaces for traits")
        }

        /**
         * Removes the specified phase from the phases set.
         */
        protected def removeFromPhasesSet(sub: SubComponent) {
            phasesSet -= sub
            phasesDescMap -= sub
        }
    }

    /**
     * A reporter that doesn't output anything to standard output. It stores all reported messages into internal
     * structures so they may retrieved later on.
     */
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
            val position = if (pos.isDefined) s"\nOn line ${pos.line} column ${pos.column}: ${pos.lineContent}" else ""

            messages += s"[$severityDescription] $msg $position"
        }
    }
}
