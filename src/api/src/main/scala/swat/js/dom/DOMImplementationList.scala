package swat.js.dom

trait DOMImplementationList {
    val length: Int

    def item(index: Int): DOMImplementation
}
