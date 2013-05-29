package swat.tests

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import swat.js.DefaultScope._

class Link(var a: Link)

@swat.remote
object Remote {
    def foo(x: Int, y: Int, z: Int): Future[Int] = future {
        x + y + z
    }

    def bar(x: (Option[String], List[Int], Link)): Future[String] = future {
        x.toString()
    }
}

object Playground extends App {
    console.log("Calling remote method Remote.foo.")

    var a1 = new Link(null)
    var a2 = new Link(a1)
    var a3 = new Link(a2)
    a1.a = a3

    Remote.bar(Tuple3(Some("test"), 3 :: 5 :: 7 :: Nil, a1)).onComplete {
        case Success(x) => console.log("Success, the returned result is " + x)
        case Failure(e) => console.log("Failure")
    }
}
