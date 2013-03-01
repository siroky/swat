package swat.api.js.dom

trait ProcessingInstruction extends Node {
    val target: String
    var data: String
}
