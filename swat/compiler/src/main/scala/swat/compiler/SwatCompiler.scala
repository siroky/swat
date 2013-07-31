package swat.compiler

import scala.tools.nsc.{SubComponent, Global, Settings}
import scala.tools.nsc.io.{File, Directory}
import tools.nsc.reporters.Reporter
import collection.mutable
import scala.tools.nsc.transform.Erasure
import scala.reflect.internal.util.Position

/**
 * A Scala compiler that, on the top of the standard Scala compiler, includes the [[swat.compiler.SwatCompilerPlugin]]
 * plugin producing JavaScript that should behave equally to the input Scala AST. Implemented as a wrapper of an
 * extended Scala compiler.
 * @param classPath Classpath for the compiled sources. If None, then the current classpath is used.
 * @param classTarget Target directory where the class files are optionally stored. If None then they're thrown away.
 * @param javaScriptTarget The target directory where the generated JavaScript files optionally are stored.
 */
class SwatCompiler(
    val classPath: Option[String],
    val classTarget: Option[String],
    val javaScriptTarget: Option[String],
    val reportOnlyErrors: Boolean = false) {

    /**
     * Compiles the specified source code.
     * @param scalaCode The Scala code to compile.
     * @return A compilation output.
     */
    def compile(scalaCode: String): CompilationOutput = {
        val sourceFile = new File (new java.io.File(s"${java.util.UUID.randomUUID}.scala"))
        try {
            sourceFile.writeAll(scalaCode)
            compile(List(sourceFile.jfile))
        } finally {
            sourceFile.delete()
        }
    }

    /**
     * Compiles the specified files.
     * @param sourceFiles The Scala source files to compile.
     * @return A compilation output.
     */
    def compile(sourceFiles: List[java.io.File]): CompilationOutput = {
        // If the classpath is unspecified, use the current.
        val urls = java.lang.Thread.currentThread.getContextClassLoader match {
            case cl: java.net.URLClassLoader => cl.getURLs.toList
            case _ => throw new CompilationException("Couldn't provide the current classpath to the compiler.")
        }
        val currentClassPath = urls.map(_.getFile).mkString(java.io.File.pathSeparator)

        // If the class target is unspecified, create a temporary directory and delete it afterwards.
        val target = new Directory(new java.io.File(classTarget.getOrElse(java.util.UUID.randomUUID.toString)))
        target.createDirectory()

        // Setup the compiler.
        val settings = new Settings()
        settings.outdir.value = target.path
        settings.classpath.value = classPath.getOrElse(currentClassPath)
        settings.deprecation.value = true
        settings.unchecked.value = true
        settings.feature.value = true
        val reporter = new SilentReporter(reportOnlyErrors)
        val compiler = new SwatGlobal(settings, reporter)
        val run = new compiler.Run()

        // Compile the sources and throw an exception if any error was reported.
        try {
            run.compile(sourceFiles.map(_.getPath))
        } catch {
            case t: Throwable => // Consume it because it should already have been reported to the reporter.
        } finally {
            if (classTarget.isEmpty) {
                target.deleteRecursively()
            }
        }

        val typeOutputs = compiler.swatPlugin.typeOutputs.toList
        if (reporter.errors.nonEmpty) {
            val (internalErrors, presentableErrors) = reporter.errors.partition(_.contains("during phase: "))
            val message = (if (presentableErrors.nonEmpty) presentableErrors else internalErrors).mkString("\n")
            throw new CompilationException(message)
        }

        // Produce the output and return the output JavaScript ASTs.
        produceJavaScript(typeOutputs)
        CompilationOutput(typeOutputs, reporter.warnings.toList, reporter.infos.toList)
    }

    /** If the javaScriptTarget is specified, produces a file for each type in the specified type outputs. */
    private def produceJavaScript(typeOutputs: List[TypeOutput]) {
        // If the javaScriptTarget is specified, create the JavaScript files there.
        javaScriptTarget.foreach { target =>
            typeOutputs.foreach { typeOutput =>
                val typeFileName = target + "/" + typeOutput.identifier.replace(".", "/") + ".swat.js"
                val typeFile = new File(new java.io.File(typeFileName))
                typeFile.parent.createDirectory()
                typeFile.writeAll(typeOutput.code)
            }
        }
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

        /** Adds the internal compiler phases to the phases set. */
        override protected def computeInternalPhases() {
            super.computeInternalPhases()

            // Alter the compiler phases.
            swatPlugin.components.foreach(c => addToPhasesSet(c, c.phaseName))
            removeFromPhasesSet(erasure)
            removeFromPhasesSet(specializeTypes) // TODO investigate
            addToPhasesSet(postponedErasure, "erase types, add interfaces for traits")
        }

        /** Removes the specified phase from the phases set. */
        protected def removeFromPhasesSet(sub: SubComponent) {
            phasesSet -= sub
            phasesDescMap -= sub
        }
    }

    /**
     * A reporter that doesn't output anything to standard output. It stores all reported messages into internal
     * structures so they may retrieved later on.
     */
    private class SilentReporter(val reportOnlyErrors: Boolean) extends Reporter {

        val errors = mutable.ListBuffer.empty[String]
        val warnings = mutable.ListBuffer.empty[String]
        val infos = mutable.ListBuffer.empty[String]

        protected def info0(pos: Position, msg: String, severity: Severity, force: Boolean) {
            val (messages, severityDescription) = severity match {
                case ERROR => (errors, "error")
                case WARNING => (warnings, "warning")
                case INFO => (infos, "info")
            }
            val text = msg.replace("\n", " ")
            val position =
                if (pos.isDefined) {
                    s" on line ${pos.line} column ${pos.column}"
                } else {
                    ""
                }

            if (severity == ERROR || !reportOnlyErrors) {
                messages += s"[$severityDescription$position]: $text"
            }
        }
    }
}
