package swat.library.java.lang

class Short(val x: scala.Short) {
    def shortValue = x
}

object Short {
    val MIN_VALUE = -32768
    val MAX_VALUE = 32767

    def valueOf(x: scala.Short) = new java.lang.Short(x)
}
