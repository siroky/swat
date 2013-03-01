package swat.api.js.dom

trait DOMImplementationSource {
    def getDOMImplementation(features: String): DOMImplementation
    def getDOMImplementationList(features: String): DOMImplementationList
}
