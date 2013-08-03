package swat.web.playground

import swat.js.DefaultScope._
import swat.client.workers._

/** The application running in the web page. */
object Playground extends App {

    // Start a new worker of the specified type and define a handler for the received messages.
    val worker = Worker.start[EchoWorker] { (sender, message) =>
        console.log(message)
    }

    // The worker is now running so a message can be sent to it.
    console.log("Sending first message.")
    worker ! "Foo"

    // And another message after five seconds.
    window.setTimeout(() => {
        console.log("Sending second message.")
        worker ! "Bar"
    }, 5000)
}

/** The application running inside a worker. */
class EchoWorker extends Worker {
    def receive(sender: WorkerRef, message: Any) {
        // Just wrap the message and send it back. A complex computation can be performed here...
        val reply = "EchoServer responding to <<" + message.toString + ">>"
        sender ! reply
    }
}


