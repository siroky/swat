package swat.api.js.workers

import swat.api.js.communication.MessageEvent

trait SharedWorkerGlobalScope extends WorkerGlobalScope {
    val name: String = ???
    var onconnect: MessageEvent[this.type] => Unit = ???
}

object SharedWorkerGlobalScope extends SharedWorkerGlobalScope
