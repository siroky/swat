package swat.client.workers

import scala.concurrent._
import ExecutionContext.Implicits.global
import swat.js.communication.MessagePort
import swat.client.json.JsonSerializer

/** Representation of a created or referenced worker. */
trait WorkerRef {

    /**
     * Sends a message to the worker that is represented by the current reference.
     * @param message The message to send.
     */
    def send(message: Any)

    /**
     * Sends a message to the worker that is represented by the current reference.
     * @param message The message to send.
     */
    def !(message: Any) {
        send(message)
    }
}

/**
 * Implementation of the [[swat.client.workers.WorkerRef]] trait that is hidden from the users.
 * @param port MessagePort of the worker.
 * @param receive Function that is invoked when a message is received from the worker.
 */
private[workers]
class WorkerReference(val port: MessagePort, val receive: (WorkerRef, Any) => Unit) extends WorkerRef {
    initialize()

    def send(message: Any) {
        port.postMessage(JsonSerializer.serialize(message))
    }

    /** Initializes the worker reference. */
    private def initialize() {
        port.onmessage = messageEvent => {
            // Deserialize the message and invoke the receive handler.
            JsonSerializer.deserialize(messageEvent.data.toString).map { message =>
                receive(this, message)
            }
        }
    }
}
