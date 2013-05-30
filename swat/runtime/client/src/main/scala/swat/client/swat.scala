package swat.client

import _root_.swat._
import _root_.swat.js

@adapter object swat {
    val controllerUrl: String = ???
    val loadedTypes: js.Array[String] = ???

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
    def access(path: String): Any = ???

    def jsArrayToScalaArray[A](a: js.Array[A]): Array[A] = ???
    def scalaArrayToJsArray[A](a: Array[A]): js.Array[A] = ???

    def serialize(value: Any): String = ???
    def findMissingTypes(value: Any): js.Array[String] = ???
    def deserialize(value: Any): Any = ???
}
