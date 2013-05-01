package swat.library.java.lang

import swat.js.Number

class Double(val x: scala.Double) {
    def doubleValue = x
}

object Double {
    val MIN_VALUE = Number.MIN_VALUE
    val MAX_VALUE = Number.MAX_VALUE
    val NaN = Number.NaN
    val POSITIVE_INFINITY = Number.POSITIVE_INFINITY
    val NEGATIVE_INFINITY = Number.NEGATIVE_INFINITY

    def valueOf(x: scala.Double) = new java.lang.Double(x)
}
