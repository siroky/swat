package swat.common.rpc

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.reflect.internal.MissingRequirementError
import scala.reflect.runtime.universe._
import swat.common.json.JsonSerializer
import swat.common.reflect.CachedMirror

@swat.ignored
object RpcDispatcher {

    val mirror = new CachedMirror
    val serializer = new JsonSerializer(mirror)

    def invoke(methodIdentifier: String, arguments: String): Future[String] = {
        future {
            val info = methodIdentifierToInfo(methodIdentifier)
            val deserializedArguments = info.method.paramss.flatten.map(_.typeSignature).toList match {
                case parameterTypes if parameterTypes.nonEmpty => {
                    // Turn the method parameter types to a tuple of the types, deserialize the arguments as a tuple
                    // and extract the parameter values from the tuple.
                    val tupleType = mirror.getClassSymbol("scala.Tuple" + parameterTypes.length).typeSignature
                    val typeParameters = tupleType.asInstanceOf[TypeRefApi].args.map(_.typeSymbol)
                    val parameterTupleType = tupleType.substituteTypes(typeParameters, parameterTypes)
                    val argumentTuple = serializer.deserialize(arguments, Some(parameterTupleType))
                    argumentTuple.asInstanceOf[Product].productIterator.toList
                }
                case Nil => Nil
            }

            // Invoke the method.
            val target = mirror.use(_.reflectModule(info.target).instance)
            val methodMirror = mirror.use(_.reflect(target)).reflectMethod(info.method)
            methodMirror(deserializedArguments: _*).asInstanceOf[Future[Any]]

        }.flatMap { result =>
            // Serialize the result and flatten the nested futures.
            result.map(serializer.serialize(_))

        }.recover {
            // If any exception occurred so far, serialize it. The serializer should always serialize a Throwable
            // without any exceptions (Note that it may even serialize exception that occurred during previous
            // serialization of successful result).
            case t: Throwable => serializer.serialize(t)
        }
    }

    def methodIdentifierToInfo(methodIdentifier: String): InvocationInfo = {
        if (methodIdentifier == null || methodIdentifier == "") {
            throw new RpcException("The method identifier must be non-empty.")
        }
        val index = methodIdentifier.lastIndexOf(".")
        if (index <= 0 || index >= (methodIdentifier.length - 1)) {
            throw new RpcException(s"The method identifier '$methodIdentifier' is invalid.")
        }

        val objectTypeFullName = methodIdentifier.take(index)
        val methodName = methodIdentifier.drop(index + 1)

        // The invocation target object.
        val target =
            try {
                mirror.getObjectSymbol(objectTypeFullName)
            } catch {
                case e: MissingRequirementError => throw new RpcException(e.getMessage)
            }

        // The method that should be invoked.
        val method = target.typeSignature.member(newTermName(methodName)) match {
            case m: MethodSymbol => m
            case NoSymbol => throw new RpcException(s"The '$objectTypeFullName' doesn't contain method '$methodName'.")
        }

        // Check whether the method is actually a remote method.
        val remoteAnnotations = List(target, method).flatMap(_.annotations.collect { case r: swat.remote => r })
        if (remoteAnnotations.isEmpty) {
            throw new RpcException(s"The method '${method.fullName}' has to be annotated with @swat.remote.")
        }

        // Check whether the method returns a future.
        if (!(method.returnType <:< typeOf[Future[_]])) {
            throw new RpcException(s"The method '${method.fullName}' has to return subtype of Future[_].")
        }

        InvocationInfo(target, method)
    }

    case class InvocationInfo(target: ModuleSymbol, method: MethodSymbol)
}
