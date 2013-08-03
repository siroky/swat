package swat.java.lang

class Boolean(val x: scala.Boolean) {
    def booleanValue = x
}

object Boolean {
    def valueOf(x: scala.Boolean) = new java.lang.Boolean(x)
}
