package swat.tests

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import swat.js.DefaultScope._

@swat.remote
object Remote {
    def foo(x: Int, y: Int, z: Int): Future[Int] = future {
        x + y + z
    }
}

object Playground extends App {
    console.log("Calling remote method Remote.foo.")

    Remote.foo(3, 5, 7).onComplete {
        case Success(x) => console.log("Success, the returned result is " + x)
        case Failure(e) => console.log("Failure")
    }
}
