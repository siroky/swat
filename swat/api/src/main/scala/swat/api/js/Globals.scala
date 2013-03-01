package swat.api.js

import swat.api.js.browser.{Json, Console, Window}
import swat.api.js.html.Document

object window extends Window
object document extends Document
object console extends Console
object JSON extends Json

object `package` {
    val Infinity: Any = ???
    val NaN: Any = ???
    val undefined: Any = ???

    def decodeURI(uri: String): String = ???
    def decodeURIComponent(uri: String): String = ???
    def encodeURI(uri: String): String = ???
    def encodeURIComponent(uri: String): String = ???
    def escape(s: String): String = ???
    def eval(js: String): Any = ???
    def isFinite(value: Any): Boolean = ???
    def isNaN(value: Any): Boolean = ???
    def parseFloat(value: Any): Double = ???
    def parseInt(value: Any): Int = ???
    def unescape(s: String): String = ???
}
