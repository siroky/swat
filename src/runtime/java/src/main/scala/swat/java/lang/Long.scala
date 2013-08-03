package swat.java.lang

class Long(val x: scala.Long) {
    def longValue = x
}

object Long {
    val MIN_VALUE = -9223372036854775808L
    val MAX_VALUE = 9223372036854775807L

    def valueOf(x: scala.Long) = new java.lang.Long(x)
}
