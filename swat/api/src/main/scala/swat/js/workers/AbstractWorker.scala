package swat.js.workers

import swat.js.events.EventTarget

trait AbstractWorker extends EventTarget {
    var onerror: ErrorEvent[this.type] => Unit = ???
}
