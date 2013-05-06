package swat.common.rpc

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.reflect.runtime.universe._
import scala.reflect.internal.MissingRequirementError
import swat.common.json.JsonSerializer

@swat.ignored
object RpcDispatcher {
    def invoke(methodIdentifier: String, arguments: String): Future[String] = {
        val mirror = runtimeMirror(getClass.getClassLoader)
        val serializer = new JsonSerializer(mirror)

        future {
            val info = methodIdentifierToInfo(methodIdentifier, mirror)
            val parsedArguments = parseArguments(arguments, info.method.paramss.flatten, mirror)
            info
        }.flatMap { data =>
            // Invoke the method.
            null.asInstanceOf[Future[Any]]
        }.map { result =>
            // Serialize the result.
            serializer.serialize(result)
        }.recover {
            // If any exception occurred so far, serialize it. The serializer should always serialize a Throwable
            // without any exceptions (Note that it may even serialize exception that occurred during previous
            // serialization of successful result).
            case t: Throwable => serializer.serialize(t)
        }
    }

    def methodIdentifierToInfo(methodIdentifier: String, mirror: Mirror): InvocationInfo = {
        if (methodIdentifier == null || methodIdentifier == "") {
            throw new RpcException("The method identifier must be non-empty.")
        }
        val index = methodIdentifier.lastIndexOf(".")
        if (index <= 0 || index >= (methodIdentifier.length - 1)) {
            throw new RpcException(s"The method identifier '$methodIdentifier' is invalid.")
        }

        val objectIdentifier = methodIdentifier.take(index)
        val methodName = methodIdentifier.drop(index + 1)

        // The invocation target object.
        val target =
            try {
                mirror.staticModule(objectIdentifier)
            } catch {
                case e: MissingRequirementError => throw new RpcException(e.getMessage)
            }

        // The method that should be invoked.
        val method = target.typeSignature.member(newTermName(methodName)) match {
            case m: MethodSymbol => m
            case NoSymbol => throw new RpcException(s"The '$objectIdentifier' doesn't contain method '$methodName'.")
        }

        InvocationInfo(target, method)
    }

    case class InvocationInfo(target: ModuleSymbol, method: MethodSymbol) {
        // Check whether the method is actually a remote method.
        val remoteAnnotations = List(target, method).flatMap(_.annotations.collect { case r: swat.remote => r })
        if (remoteAnnotations.isEmpty) {
            throw new RpcException(s"The method '${method.fullName}' has to be annotated with @swat.remote.")
        }

        // Check whether the method returns a future.
        if (!(method.returnType <:< typeOf[Future[_]])) {
            throw new RpcException(s"The method '${method.fullName}' has to return subtype of Future[_].")
        }
    }

    private def parseArguments(arguments: String, parameters: List[Symbol], mirror: Mirror): List[Any] = {
        Nil
    }
}
