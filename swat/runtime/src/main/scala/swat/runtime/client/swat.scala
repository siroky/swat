package swat.runtime.client

import _root_.swat.api._
import _root_.swat.api.js

@adapter
object swat {
    def isUndefined(obj: Any): Boolean = ???
    def isDefined(obj: Any): Boolean = ???
    def isJsObject(obj: Any): Boolean = ???
    def isJsArray(obj: Any): Boolean = ???
    def isJsFunction(obj: Any): Boolean = ???
    def isJsString(obj: Any): Boolean = ???
    def isJsNumber(obj: Any): Boolean = ???
    def isJsBoolean(obj: Any): Boolean = ???
    def isInteger(obj: Any): Boolean = ???
    def isChar(obj: Any): Boolean = ???
    def isSwatObject(obj: Any): Boolean = ???

    val loadedTypes: js.Array[String] = ???
}
