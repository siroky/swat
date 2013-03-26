package swat.runtime.client.java.lang

class Throwable(message: java.lang.String, cause: java.lang.Throwable) {
    def this() = this(null, null)
    def this(message: java.lang.String) = this(message, null)
    def this(cause: java.lang.Throwable) = this(if (cause == null) null else cause.toString, cause)

    def getMessage = message
    def getCause = cause
}
