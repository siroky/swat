package swat.internal.java.util.concurrent

class TimeoutException(message: java.lang.String) extends RuntimeException(message) {
     def this() = this(null)
}
