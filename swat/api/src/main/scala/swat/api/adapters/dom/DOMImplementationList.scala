package swat.api.adapters.dom

trait DOMImplementationList {
    val length: Int

    def item(index: Int): DOMImplementation
}
