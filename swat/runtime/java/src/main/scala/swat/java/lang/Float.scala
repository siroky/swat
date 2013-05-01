package swat.java.lang

import swat.js.Number

class Float(val x: scala.Float) {
    def floatValue = x
}

object Float {
    val MIN_VALUE = Number.MIN_VALUE
    val MAX_VALUE = Number.MAX_VALUE
    val NaN = Number.NaN
    val POSITIVE_INFINITY = Number.POSITIVE_INFINITY
    val NEGATIVE_INFINITY = Number.NEGATIVE_INFINITY

    def valueOf(x: scala.Float) = new java.lang.Float(x)
}
