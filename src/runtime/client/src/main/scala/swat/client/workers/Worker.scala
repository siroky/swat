package swat.client.workers

import swat.js._
import swat.js.workers.SharedWorker

/**
 * An application that is started as a worker, so it can receive messages from other workers and reply to them. Meant
 * to be mixed to a class that implements the behavior. Note that a worker has to be started using the
 * [[swat.client.workers.Worker.start()]] method, e.g. Worker.start[MyWorker]. Instantiating the worker, e.g.
 * new MyWorker() won't make it run concurrently.
 */
trait Worker extends App {
    initialize()

    /**
     * A method that is invoked when the worker receives a message. Has to be implemented by the class that mixes in
     * the [[swat.client.workers.Worker]] trait.
     * @param sender Reference to a worker that sent the message.
     * @param message The message.
     */
    def receive(sender: WorkerRef, message: Any)

    /** Initializes the worker application. */
    private def initialize() {
        SharedWorkerScope.self.onconnect = e => new WorkerReference(e.ports.pop(), receive)
    }
}

/** A factory for web workers. */
object Worker {
    /**
     * Creates and starts a new worker.
     * @param receive The function that is invoked when a message is received from the created worker. First parameter
     *                is always a reference to the created worker, the second parameter is the message.
     * @tparam A Type of the worker that mixes-in the [[swat.client.workers.Worker]]
     * @return A reference to the created worker.
     */
    def start[A <: Worker](receive: (WorkerRef, Any) => Unit): WorkerRef = {
        val typeIdentifier = native("arguments[1].$class.typeIdentifier")
        val worker = new SharedWorker(swat.client.swat.controllerUrl + "/app/" + typeIdentifier, typeIdentifier)
        val workerRef = new WorkerReference(worker.port, receive)
        worker.port.start()
        workerRef
    }
}


