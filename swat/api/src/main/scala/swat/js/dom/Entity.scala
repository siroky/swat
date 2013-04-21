package swat.js.dom

trait Entity extends Node {
    val publicId: String
    val systemId: String
    val notationName: String
    val inputEncoding: String
    val xmlEncoding: String
    val xmlVersion: String
}
