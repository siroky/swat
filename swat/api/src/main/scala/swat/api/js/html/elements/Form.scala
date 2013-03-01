package swat.api.js.html.elements

import swat.api.js.html._
import swat.api.js.events.Event

trait Form extends Element {
    var acceptCharset: String
    var action: String
    var enctype: String
    var length: Int
    var method: String
    var name: String
    var onreset: Event[this.type] => Boolean
    var onsubmit: Event[this.type] => Boolean

    def reset()
    def submit()
}
