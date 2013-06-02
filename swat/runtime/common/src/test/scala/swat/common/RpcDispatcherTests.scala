package swat.common

import org.scalatest.FunSuite
import swat.common.rpc.RpcDispatcher
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class TestException(val message: String) extends Exception(message)

@swat.remote
object TestRemote {
    def foo(x: Int, y: Int, z: Int): Future[Int] = future {
        x + y + z
    }

    def bar: Future[Int] = future {
        throw new TestException("Custom exception")
    }
}

class RpcDispatcherTests extends FunSuite {

    test("Remote methods are invoked.") {
        val methodFullName = "swat.common.TestRemote.foo"
        val arguments = """
            {
                "$value":{"$ref":0},
                "$objects": [
                    {
                        "$id":0,
                        "$type":"scala.Tuple3",
                        "_1":3,
                        "_2":5,
                        "_3":7
                    }
                ]
            }
                        """
        assert(invokeRemote(methodFullName, arguments) == 15)
    }

    test("Exceptions thrown by the infrastructure are processed.") {
        val result = invokeRemote("foo", "", deserialize = false)
        assert(result == """{"$value" : {"$ref" : 0},"$objects" : [ {"$id" : 0,"$type" : "swat.common.rpc.RpcException","message" : "The method identifier 'foo' is invalid."} ]}""")
    }

    test("Exceptions thrown by remote methods are processed.") {
        val methodFullName = "swat.common.TestRemote.bar"
        val arguments = "null"
        val result = invokeRemote(methodFullName, arguments, deserialize = false)
        assert(result == """{"$value" : {"$ref" : 0},"$objects" : [ {"$id" : 0,"$type" : "swat.common.TestException","message" : "Custom exception"} ]}""")
    }

    private def invokeRemote(methodFullName: String, arguments: String, deserialize: Boolean = true): Any = {
        val result = Await.result(RpcDispatcher.invoke(methodFullName, arguments), Duration("1 min"))
        if (deserialize) {
            RpcDispatcher.serializer.deserialize(result)
        } else {
            result.lines.map(_.trim).mkString
        }
    }
}


