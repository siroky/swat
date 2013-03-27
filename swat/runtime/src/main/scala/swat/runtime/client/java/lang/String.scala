package swat.runtime.client.java.lang

import swat.api.native
import swat.api.js.RegExp

class String

object String {
    @native("return s.length;")
    def length(s: java.lang.String) = ???

    def isEmpty(s: java.lang.String) = s.length == 0

    @native("return s.indexOf(c);")
    def indexOf(s: java.lang.String, c: scala.Char): Int = ???

    @native("return s.lastIndexOf(c);")
    def lastIndexOf(s: java.lang.String, c: scala.Char): Int = ???

    def matches(s: java.lang.String, pattern: java.lang.String): Boolean = new RegExp(pattern, "").test(s)
}
