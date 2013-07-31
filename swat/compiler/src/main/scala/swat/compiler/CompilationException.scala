package swat.compiler

/** An exception thrown by the [[swat.compiler.SwatCompiler]] in case of error. */
class CompilationException(val message: String = "", val cause: Throwable = null) extends Exception(message, cause)
