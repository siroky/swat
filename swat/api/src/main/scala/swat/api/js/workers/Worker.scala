package swat.api.js.workers

import swat.api.js.communication.MessageExchanger

class Worker(scriptUrl: String) extends AbstractWorker with MessageExchanger {
    def terminate() {}
}
