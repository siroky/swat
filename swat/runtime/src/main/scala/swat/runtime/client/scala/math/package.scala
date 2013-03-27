package swat.runtime.client.scala

import swat.api.js.Math

package object math {
    val E = Math.E
    val Pi = Math.PI

    def abs(x: scala.Double) = Math.abs(x)
    def acos(x: scala.Double) = Math.acos(x)
    def asin(x: scala.Double) = Math.asin(x)
    def atan(x: scala.Double) = Math.atan(x)
    def atan2(x: scala.Double) = Math.atan2(x)
    def ceil(x: scala.Double) = Math.ceil(x)
    def cos(x: scala.Double) = Math.cos(x)
    def exp(x: scala.Double) = Math.exp(x)
    def floor(x: scala.Double) = Math.floor(x)
    def log(x: scala.Double) = Math.log(x)
    def max(x: scala.Double, y: scala.Double) = Math.max(x, y)
    def min(x: scala.Double, y: scala.Double) = Math.min(x, y)
    def pow(x: scala.Double, y: scala.Double) = Math.pow(x, y)
    def sqrt(x: scala.Double) = Math.sqrt(x)
    def random() = Math.random()
    def round(x: scala.Double) = Math.round(x)
    def sin(x: scala.Double) = Math.sin(x)
    def tan(x: scala.Double) = Math.tan(x)
    def signum(x: scala.Double) = if (x < 0) - 1 else if (x > 0) 1 else 0
}
