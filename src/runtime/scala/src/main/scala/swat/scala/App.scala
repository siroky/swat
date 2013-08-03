package swat.scala

import swat.js

trait App {
    protected def args: Array[String] = js.native {
        "return swat.startupArgs;"
    }
}
