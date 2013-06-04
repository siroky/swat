package swat.client.rpc

import scala.concurrent._
import ExecutionContext.Implicits.global
import swat.js.applications.{ActiveXObject, XMLHttpRequest}
import swat.client.swat
import _root_.swat.client.json.JsonSerializer
import _root_.swat.common.rpc.RpcException

/** An object responsible for forwarding of remote method calls to the server. */
object RpcProxy {

    /**
     * Invokes the specified remote method. Serializes the arguments, sends them together with the methodIdentifier to
     * the server using AJAX and returns Future of the result. When the result arrives, deserializes it and completes
     * the returned Future.
     */
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

    /**
     * Processes the AJAX response and returns Future of the result. When the deserialization finishes, completes the
     * returned Future.
     */
    private def processResponse(response: String, status: Int): Future[Any] = {
        if (status != 200 && status != 500) {
            Future.failed(new RpcException(s"The remote method call exited with status code " + status + "."))
        } else {
            try {
                // If the response is a RpcException, turn it into a failed future.
                JsonSerializer.deserialize(response).map(_ match {
                    case r: RpcException => throw r
                    case s => s
                })
            } catch {
                case t: Throwable => Future.failed(new RpcException(
                    "An exception occurred during deserialization of the remote method result (" + response + ")."
                ))
            }
        }
    }
}
