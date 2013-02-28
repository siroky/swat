package swat.api.adapters.dom

trait ProcessingInstruction extends Node {
    val target: String
    var data: String
}
