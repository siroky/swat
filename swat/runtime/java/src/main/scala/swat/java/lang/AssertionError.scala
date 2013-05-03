package swat.java.lang

class AssertionError(message: Any) extends Throwable(message.toString) {
    def this() = this(null)
}
