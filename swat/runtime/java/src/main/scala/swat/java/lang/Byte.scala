package swat.library.java.lang

class Byte(val x: scala.Byte) {
    def byteValue = x
}

object Byte {
    val MIN_VALUE = -128
    val MAX_VALUE = 127

    def valueOf(x: scala.Byte) = new java.lang.Byte(x)
}
