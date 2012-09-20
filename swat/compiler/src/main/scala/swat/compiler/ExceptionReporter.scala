package swat.compiler

import tools.nsc.reporters.Reporter
import reflect.internal.util.{NoPosition, Position}

class CompilationException(message: String = "", cause: Throwable = null) extends Exception(message, cause)

/**
 * A compiler error reporter that reports errors by throwing exceptions instead of writing it to the standard
 * output.
 */
private class ExceptionReporter extends Reporter
{
    protected def info0(pos: Position, msg: String, severity: this.type#Severity, force: Boolean) {
        if (severity != INFO) {
            val severityDescription = severity match {
                case WARNING => "Warning"
                case ERROR => "Error"
            }
            val position = pos match {
                case NoPosition => ""
                case _ => " on line %s, column %s".format(pos.line, pos.column)
            }
            throw new CompilationException("%s%s: %s".format(severityDescription, position, msg))
        }
    }
}
