package swat.tests

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import swat.js.DefaultScope._

class Link(var a: Link)

class Parent {
    override def toString = "parent"
}
class Child extends Parent {
    override def toString = "child"
}

@swat.remote
object Remote {
    def foo(x: Int, y: Int, z: Int): Future[Int] = future {
        x + y + z
    }

    def bar(x: (Option[String], List[Int], Link)): Future[String] = future {
        x.toString()
    }

    def baz(): Future[Parent] = future {
        new Child
    }
}

object Playground extends App {

    testTypeLoading()

    def testSerialization() {
        console.log("Calling remote method Remote.bar.")

        val a1 = new Link(null)
        val a2 = new Link(a1)
        val a3 = new Link(a2)
        a1.a = a3

        Remote.bar(Tuple3(Some("test"), 3 :: 5 :: 7 :: Nil, a1)).onComplete {
            case Success(x) => console.log("Success, the returned result is " + x)
            case Failure(e) => console.log("Failure")
        }
    }

    def testTypeLoading() {
        console.log("Calling remote method Remote.baz.")
        Remote.baz().onComplete {
            case Success(x) => console.log("Success, the returned result is " + x.toString)
            case Failure(e) => console.log("Failure")
        }
    }
}
