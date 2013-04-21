package swat.js

import swat.js.applications.Json

trait Scope {
    val JSON: Json = ???

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

object Scope extends Scope
