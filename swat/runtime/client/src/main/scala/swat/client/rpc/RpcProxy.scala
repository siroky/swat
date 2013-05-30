package swat.client.rpc

import scala.concurrent._
import ExecutionContext.Implicits.global
import swat.js.applications.{ActiveXObject, XMLHttpRequest}
import swat.client.swat
import _root_.swat.client.json.JsonSerializer
import _root_.swat.common.rpc.RpcException

object RpcProxy {

    def invoke(methodIdentifier: String, args: Product): Future[Any] = {
        val promise = Promise[Any]()
        val result = promise.future

        val request =
            if (swat.isDefined(swat.access("XMLHttpRequest"))) {
                new XMLHttpRequest()
            } else {
                new ActiveXObject("Msxml2.XMLHTTP")
            }

        request.onreadystatechange = { _ =>
            if (request.readyState == 4 && request.status != 0) {
                // The request is done and the status isn't invalid. After the processing of the response is done,
                // complete the result promise.
                processResponse(request.responseText, request.status).onComplete(promise.complete(_))
            }
        }
        request.open("POST", swat.controllerUrl + "/rpc/" + methodIdentifier, async = true)
        request.setRequestHeader("Content-Type", "application/json")
        request.send(JsonSerializer.serialize(args))

        result
    }

    private def processResponse(response: String, status: Int): Future[Any] = {
        if (status != 200 && status != 500) {
            Future.failed(new RpcException(s"The RPC exited with status code $status."))
        } else {
            try {
                // If the response is a successfully deserialized Throwable, turn it into a failed future.
                JsonSerializer.deserialize(response).map(_ match {
                    case t: Throwable => throw t
                    case s => s
                })
            } catch {
                case e: Throwable => {
                    Future.failed(new RpcException(s"RPC result deserialization error (${e.getMessage}})."))
                }
            }
        }
    }
}
