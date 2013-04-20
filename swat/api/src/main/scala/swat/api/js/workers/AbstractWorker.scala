package swat.api.js.workers

import swat.api.js.events.EventTarget

trait AbstractWorker extends EventTarget {
    var onerror: ErrorEvent[this.type] => Unit = ???
}
