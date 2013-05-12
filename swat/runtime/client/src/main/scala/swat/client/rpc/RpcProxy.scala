package swat.client.rpc

import scala.concurrent.{Promise, Future}
import scala.util.{Success, Failure, Try}
import swat.js.applications.{ActiveXObject, XMLHttpRequest}
import swat.client.swat._
import swat.client.json.JsonSerializer
import swat.common.rpc.RpcException

object RpcProxy {

    def invoke(methodIdentifier: String, args: Product): Future[Any] = {
        val promise = Promise[Any]()
        val result = promise.future

        val request =
            if (isDefined(access("XMLHttpRequest"))) {
                new XMLHttpRequest()
            } else {
                new ActiveXObject("Msxml2.XMLHTTP")
            }

        request.onreadystatechange = { _ =>
            if (request.readyState == 4 && request.status != 0) {
                // The request is done and the status isn't invalid.
                promise.complete(processResponse(request.responseText, request.status))
            }
        }
        request.open("POST", controllerUrl + "/rpc/" + methodIdentifier, async = true)
        request.setRequestHeader("Content-Type", "application/x-www-form-urlencoded")
        request.send(JsonSerializer.serialize(args))

        result
    }

    private def processResponse(response: String, status: Int): Try[Any] = {
        if (status != 200 && status != 500) {
            Failure(new RpcException(s"The RPC exited with status code $status."))
        } else {
            try {
                // Deserialize the response and if it's a throwable, return Failure. Otherwise return Success.
                JsonSerializer.deserialize(response) match {
                    case t: Throwable => Failure(t)
                    case x => Success(x)
                }
            } catch {
                case e: Throwable => Failure(new RpcException(s"RPC result deserialization error (${e.getMessage}})."))
            }
        }
    }
}
