package swat.js.communication

import swat.js.events.EventTarget
import swat.js.dom.Transferable

trait MessagePort extends EventTarget with MessageExchanger with Transferable {
    def start() {}
    def close() {}
}
