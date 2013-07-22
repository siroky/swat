package swat.java.lang

import swat.js.RegExp
import swat.js

object String {
    def length(s: String) = js.native {
        "return s.length;"
    }

    def isEmpty(s: String) = s.length == 0

    def indexOf(s: String, c: Char): Int = js.native {
        "return s.indexOf(c);"
    }

    def lastIndexOf(s: String, c: Char): Int = js.native {
        "return s.lastIndexOf(c);"
    }

    def startsWith(s: String, x: String): Boolean = js.native {
        "return s.indexOf(x) === 0;"
    }

    def matches(s: String, pattern: String): scala.Boolean = new RegExp(pattern, "").test(s)

    def valueOf(x: String) = x
}
