package swat.js.workers

import swat.js.communication.MessageEvent

trait SharedWorkerGlobalScope extends WorkerGlobalScope {
    val name: String = ???
    var onconnect: MessageEvent[this.type] => Unit = ???
}
