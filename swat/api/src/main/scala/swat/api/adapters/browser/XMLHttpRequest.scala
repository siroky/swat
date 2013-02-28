package swat.api.adapters.browser

import swat.api.adapters.events._
import swat.api.adapters.dom.Document

class XMLHttpRequest extends EventTarget {
    val readyState: Int = ???
    val status: Int = ???
    val statusText: String = ???
    val response: Any = ???
    val responseText: String = ???
    val responseXML: Document = ???
    var onloadstart: Event[this.type] => Unit = ???
    var onprogress: Event[this.type] => Unit = ???
    var onabort: Event[this.type] => Unit = ???
    var onerror: Event[this.type] => Unit = ???
    var onload: Event[this.type] => Unit = ???
    var ontimeout: Event[this.type] => Unit = ???
    var onloadend: Event[this.type] => Unit = ???
    var onreadystatechange: Event[this.type] => Unit = ???

    def open(method: String, url: String) {}
    def open(method: String, url: String, async: Boolean) {}
    def open(method: String, url: String, async: Boolean, username: String, password: String) {}
    def setRequestHeader(header: String, value: String) {}
    def send(data: String) {}
    def abort() {}
    def getResponseHeader(header: String): String = ???
}
