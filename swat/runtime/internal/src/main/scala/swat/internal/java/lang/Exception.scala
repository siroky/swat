package swat.internal.java.lang

class Exception(message: java.lang.String, cause: java.lang.Throwable) extends Throwable(message, cause) {
     def this() = this(null, null)
     def this(message: java.lang.String) = this(message, null)
     def this(cause: java.lang.Throwable) = this(if (cause == null) null else cause.toString, cause)
}
