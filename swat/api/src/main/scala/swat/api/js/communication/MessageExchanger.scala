package swat.api.js.communication

import swat.api.js
import swat.api.js.dom.Transferable
import swat.api.js.events.EventTarget

trait MessageExchanger { self: EventTarget =>
    var onmessage: MessageEvent[this.type] => Unit = ???

    def postMessage(message: Any) {}
    def postMessage(message: Any, transfer: js.Array[Transferable]) {}
}
