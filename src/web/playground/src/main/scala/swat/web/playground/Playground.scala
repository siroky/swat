package swat.web.playground

import swat.js.DefaultScope._
import swat.client.workers._

/** The application running in the web page. */
object Playground extends App {

    window.alert("Hello World!")

    // Start a new worker of the specified type and define a handler for the received messages.
    val worker = Worker.start[EchoWorker] { (sender, message) =>
        console.log("EchoWorker responded: " + message)
    }

    // The worker is now running so a message can be sent to it.
    console.log("Sending message Foo.")
    worker ! "Foo"

    // And another message after five seconds.
    window.setTimeout(() => {
        console.log("Sending message Bar.")
        worker ! "Bar"
    }, 5000)
}

/** The application running inside a worker. */
class EchoWorker extends Worker {
    def receive(sender: WorkerRef, message: Any) {
        // Just wrap the message and send it back. A complex computation can be performed here...
        val response = "<<" + message.toString + ">>"
        sender ! response
    }
}

// After five seconds, the console output is:
//   Sending message Foo.
//   EchoWorker responded: <<Foo>>
//   Sending message Bar.
//   EchoWorker responded: <<Bar>>
