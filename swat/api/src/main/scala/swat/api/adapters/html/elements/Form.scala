package swat.api.adapters.html.elements

import swat.api.adapters.html._
import swat.api.adapters.events.Event

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
