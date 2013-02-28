package swat.api.adapters.dom

trait Notation extends Node {
    val publicId: String
    val systemId: String
}
