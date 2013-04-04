package swat.runtime.client.scala

import swat.api.js
import swat.api.native

class Array[T](_length: Int) extends java.io.Serializable with java.lang.Cloneable {

    private var jsArray = new js.Array[T]()

    def length: Int = jsArray.length

    @native("return $self.$fields.jsArray[i];")
    def apply(i: Int): T = ???

    @native("$self.$fields.jsArray[i] = x;")
    def update(i: Int, x: T) { }

    override def clone(): Array[T] = {
        val c = new Array[T](jsArray.length)
        c.jsArray = jsArray.splice(0, jsArray.length)
        c
    }
}
