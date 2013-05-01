package swat.library.scala

trait App {
    @swat.native("return swat.startupArgs;")
    protected def args: Array[String] = ???
}
