package swat.java.lang

class Float(val x: scala.Float) {
    def floatValue = x
}

object Float {
    val MIN_VALUE = swat.js.Number.MIN_VALUE
    val MAX_VALUE = swat.js.Number.MAX_VALUE
    val NaN = swat.js.Number.NaN
    val POSITIVE_INFINITY = swat.js.Number.POSITIVE_INFINITY
    val NEGATIVE_INFINITY = swat.js.Number.NEGATIVE_INFINITY

    def valueOf(x: scala.Float) = new java.lang.Float(x)
}
