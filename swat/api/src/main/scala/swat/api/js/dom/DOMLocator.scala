package swat.api.js.dom

trait DOMLocator {
    val lineNumber: Int
    val columnNumber: Int
    val byteOffset: Int
    val utf16Offset: Int
    val relatedNode: Node
    val uri: String
}
