package swat.common

import org.scalatest.FunSuite
import swat.common.rpc.RpcDispatcher
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import play.api.libs.json.{JsArray, JsNumber, JsObject, Json}
import swat.common.json.JsonSerializer

@swat.remote
object TestRemote {
    def foo(x: Int, y: Int, z: Int): Future[Int] = future {
        x + y + z
    }
}

class RpcDispatcherTests extends FunSuite {

    test("Remote methods are invoked") {
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

        val dispatcher = new RpcDispatcher
        val result = Await.result(dispatcher.invoke("swat.common.TestRemote.foo", arguments), Duration("1 min"))
        val deserializedResult = dispatcher.serializer.deserialize(result)
        assert(deserializedResult == 15)
    }
}


