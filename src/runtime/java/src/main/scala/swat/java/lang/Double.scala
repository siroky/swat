package swat.java.lang

class Double(val x: scala.Double) {
    def doubleValue = x
}

object Double {
    val MIN_VALUE = swat.js.Number.MIN_VALUE
    val MAX_VALUE = swat.js.Number.MAX_VALUE
    val NaN = swat.js.Number.NaN
    val POSITIVE_INFINITY = swat.js.Number.POSITIVE_INFINITY
    val NEGATIVE_INFINITY = swat.js.Number.NEGATIVE_INFINITY

    def valueOf(x: scala.Double) = new java.lang.Double(x)
}
