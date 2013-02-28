package swat.api.adapters.dom

trait DOMErrorHandler {
    def handleError(error: DOMError): Boolean
}
