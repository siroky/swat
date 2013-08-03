package swat.js.workers

import swat.js.communication.MessageExchanger

class Worker(scriptUrl: String) extends AbstractWorker with MessageExchanger {
    def terminate() {}
}
