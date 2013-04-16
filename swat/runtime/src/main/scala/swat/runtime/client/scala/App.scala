package swat.runtime.client.scala

trait App {

    @swat.api.native("return swat.startupArgs;")
    protected def args: Array[String] = ???
}
