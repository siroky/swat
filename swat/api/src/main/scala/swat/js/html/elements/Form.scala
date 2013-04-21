package swat.js.html.elements

import swat.js.html._
import swat.js.events.Event

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
