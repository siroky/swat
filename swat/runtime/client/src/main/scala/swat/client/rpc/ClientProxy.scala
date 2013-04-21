package swat.runtime.client.rpc

import swat.runtime.client.swat._
import swat.js.Scope._
import swat.js.applications.{ActiveXObject, XMLHttpRequest}
import scala.concurrent.{Promise, Future}

object ClientProxy {
    /*
    def invokeMethod(fullName: String, args: Array[Any]): Future[Any] = {
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
                processRequestResult(request, successCallback, exceptionCallback)
            }
        }
        request.open("POST", controllerUrl + "/rpc/" + fullName, async = true)
        request.setRequestHeader("Content-Type", "application/x-www-form-urlencoded")
        request.send(serializeArgs(args))



        result
    }

    private def serializeArgs(args: Array[Any]): String = {

        val result =
            if (List(200, 500).contains(request.status)) {
                try {
                    deserializer.deserialize(js.eval("(" + request.responseText + ")"))
                } catch {
                    case error => {
                        val description = error match {
                            case e: RpcException => e.message
                            case _ => error.toString
                        }
                        new RpcException("Exception during deserialization of the remote method result.", description)
                    }
                }
            } else {
                new RpcException("The RPC call exited with status code " + request.status + ".")
            }

        result match {
            case throwable: Throwable => {
                onException(throwable)
                throwable
            }
            case value => {
                onSuccess(value)
                value
            }
        }
    }

    private def createRequestBody(procedureName: String, parameters: ArrayBuffer[Any],
        parameterTypeNames: ArrayBuffer[String]): String = {

        // Append the procedure name to the request body.
        val requestBody = new ArrayBuffer[String]()
        requestBody += "method=" + procedureName

        // Append the parameter types to the request body.
        requestBody += parameterSeparator + "paramTypes="
        requestBody += js.encodeURIComponent(parameterTypeNames.map(jsonEscapeAndQuote).mkString("[", ",", "]"))

        // Append the parameters to the request body.
        var index = -1
        parameters.foreach { parameterValue =>
            index += 1
            requestBody += parameterSeparator + index + "="
            requestBody += js.encodeURIComponent(processParameter(parameterTypeNames(index), parameterValue))
        }

        requestBody.mkString
    }

    private def processParameter(typeName: String, value: Any): String = {
        value match {
            case s: String => s
            case s: StringOps => s.toString
            case items: Seq[_] => {
                val escapedItems: Seq[String] =
                    if (typeName.endsWith("[scala.String]") || typeName.endsWith("[java.lang.String]")) {
                        items.map(item => jsonEscapeAndQuote(item.toString))
                    } else {
                        items.map(_.toString)
                    }
                escapedItems.mkString("[", ",", "]")
            }
            case x => x.toString
        }
    }

    private def jsonEscapeAndQuote(value: String): String = "\"" + jsonEscape(value) + "\""

    @javascript("""
        return value.replace('\\', '\\\\').replace('"', '\\"').replace("'", "\\'");
                """)
    private def jsonEscape(value: String): String = ""*/
}
