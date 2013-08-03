package swat.js.communication

import swat.js
import swat.js.dom.Transferable
import swat.js.events.EventTarget

trait MessageExchanger { self: EventTarget =>
    var onmessage: MessageEvent[this.type] => Unit = ???

    def postMessage(message: Any) {}
    def postMessage(message: Any, transfer: js.Array[Transferable]) {}
}
