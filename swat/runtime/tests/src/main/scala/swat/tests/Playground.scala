package swat.tests

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import swat.js.DefaultScope._

class Link(var a: Link)

object Link {
    def cycle: Link = {
        val a1 = new Link(null)
        val a2 = new Link(a1)
        val a3 = new Link(a2)
        a1.a = a3
        a1
    }
}

class Parent {
    override def toString = "parent"
}
class Child extends Parent {
    override def toString = "child"
}

object Complex {
    type Type = (Option[String], List[Int], Link)
    def sample: Complex.Type = Tuple3(Some("test"), 3 :: 5 :: 7 :: Nil, Link.cycle)
}

@swat.remote
object Remote {
    def sum(x: Int, y: Int, z: Int): Future[Int] = future {
        x + y + z
    }

    def complexParams(x: Complex.Type): Future[String] = future {
        x.toString()
    }

    def complexResult(): Future[Complex.Type] = future {
        Complex.sample
    }

    def subType(): Future[Parent] = future {
        new Child
    }
}

object Playground extends App {

    console.log("Calling method Remote.sum.")
    Remote.sum(3, 5, 7).onComplete {
        case Success(x) => console.log("Remote.sum success, the returned result is " + x)
        case Failure(e) => console.log("Remote.sum failure")
    }

    console.log("Calling method Remote.complexParams.")
    Remote.complexParams(Complex.sample).onComplete {
        case Success(x) => console.log("Remote.complexParams success, the returned result is " + x)
        case Failure(e) => console.log("Remote.complexParams failure")
    }

    console.log("Calling method Remote.complexResult.")
    Remote.complexResult().onComplete {
        case Success(x) => console.log("Remote.complexResult success, the returned result is " + x)
        case Failure(e) => console.log("Remote.complexResult failure")
    }

    console.log("Calling method Remote.subType.")
    Remote.subType().onComplete {
        case Success(x) => console.log("Remote.subType success, the returned result is " + x)
        case Failure(e) => console.log("Remote.subType Failure")
    }
}
