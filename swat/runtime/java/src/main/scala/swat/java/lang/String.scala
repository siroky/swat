package swat.java.lang

import swat.native
import swat.js.RegExp

object String {
    @native("return s.length;")
    def length(s: String) = ???

    def isEmpty(s: String) = s.length == 0

    @native("return s.indexOf(c);")
    def indexOf(s: String, c: Char): Int = ???

    @native("return s.lastIndexOf(c);")
    def lastIndexOf(s: String, c: Char): Int = ???

    @native("return s.indexOf(x) === 0;")
    def startsWith(s: String, x: String): Boolean = ???

    def matches(s: String, pattern: String): scala.Boolean = new RegExp(pattern, "").test(s)

    def valueOf(x: String) = x
}
