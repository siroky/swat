package swat.api.js.communication

import swat.api.js.events.EventTarget
import swat.api.js.dom.Transferable

trait MessagePort extends EventTarget with MessageExchanger with Transferable {
    def start() {}
    def close() {}
}
