package swat.java.lang

class Integer(val x: scala.Int) {
    def intValue = x
}

object Integer {
    val MIN_VALUE = -2147483648
    val MAX_VALUE = 2147483647

    def valueOf(x: scala.Int) = new java.lang.Integer(x)
}
