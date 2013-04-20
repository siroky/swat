package swat.api.js.workers

import swat.api.js.communication.MessagePort

class SharedWorker(scriptUrl: String, name: String) extends AbstractWorker {
    def this(scriptUrl: String) = this(scriptUrl, "")

    val port: MessagePort = ???
}
