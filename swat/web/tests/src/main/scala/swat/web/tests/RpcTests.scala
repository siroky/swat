package swat.web.tests

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.Tuple3
import scala.Some
import swat.js.DefaultScope._
import scala.util.{Failure, Success}
import swat.common.rpc.{Cause, RpcException}

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
object TestRemote {
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

    def exception(): Future[Int] = future {
        throw new IllegalStateException("Foo.")
    }
}

class RpcTests extends TestSuite {
    def test() {
        val sumMessage = "Simple remote method call."
        TestRemote.sum(3, 5, 7).onComplete {
            case Success(x) => assert(x == 15, sumMessage)
            case _ => fail(sumMessage)
        }

        val complexParamsMessage = "RPC call with complex parameters."
        TestRemote.complexParams(Complex.sample).onComplete {
            case Success(x) => assert(x.startsWith("(Some(test),List(3, 5, 7),swat.web.tests.Link@"), complexParamsMessage)
            case _ => fail(complexParamsMessage)
        }

        val complexResultMessage = "Remote method call with complex result."
        TestRemote.complexResult().onComplete {
            case Success((_: Option[_], _: List[_], a: Link)) if a.a.a.a == a => success(complexResultMessage)
            case _ => console.log(complexResultMessage)
        }

        val subTypeMessage = "Type loading during deserialization."
        TestRemote.subType().onComplete {
            case Success(x) if x.toString == "child" => success(subTypeMessage)
            case _ => fail(subTypeMessage)
        }

        val exceptionMessage = "Exception thrown by the remote method."
        TestRemote.exception().onComplete {
            case Failure(r: RpcException) => r.cause match {
                case Some(Cause("java.lang.IllegalStateException", "Foo.", _, None)) => success(exceptionMessage)
                case _ => fail(exceptionMessage)
            }
            case _ => fail(exceptionMessage)
        }
    }
}
