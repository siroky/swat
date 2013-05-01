package swat.library.java.lang

class Character(val x: scala.Char) {
    def charValue = x
}

object Character {
    val MIN_VALUE = 0
    val MAX_VALUE = 65535

    def valueOf(x: scala.Char) = new java.lang.Character(x)
}
