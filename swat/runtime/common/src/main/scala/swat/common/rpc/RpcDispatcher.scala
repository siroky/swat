package swat.common.rpc

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.reflect.runtime.universe._
import swat.common.json.JsonSerializer
import swat.common.reflect.ReflectionCache

/** An exception of the [[swat.common.rpc.RpcDispatcher]]. */
class RpcException(val message: String, val cause: Option[Cause] = None) extends Exception(message)

/** Serializable information about an exception. */
case class Cause(exceptionTypeName: String, message: String, stackTrace: String, cause: Option[Cause] = None)

/**
 * A dispatcher of remote method calls to the singleton objects. Responsible for both deserialization of the method
 * arguments and serialization of the result.
 */
@swat.ignored
object RpcDispatcher {

    val cache = new ReflectionCache
    val mirror = cache.mirror
    val serializer = new JsonSerializer(cache)

    /**
     * Invokes the specified remote method. Firstly verifies that the method exists and is invokable. Then deserializes
     * the method arguments and invokes the method using reflection. Serializes the result of the invocation and
     * returns it.
     * @param methodIdentifier Full identifier of the method. For example "my.package.RemoteObject.foo".
     * @param serializedArguments Arguments of the method serialized as a tuple in the format specified in the
     *                            [[swat.common.json.JsonSerializer]].
     */
    def invoke(methodIdentifier: String, serializedArguments: String): Future[String] = {
        future {
            // Performing all the operations inside the future so any exception thrown either here or inside the invoked
            // method can be finally processed in the recover block.
            mirror.synchronized {
                val (target, method) = processMethodIdentifier(methodIdentifier)
                val arguments = deserializeArguments(serializedArguments, method)
                val methodMirror = mirror.reflect(target).reflectMethod(method)

                // Invoke the method and serialize the result
                val result = methodMirror(arguments: _*).asInstanceOf[Future[Any]]
                result.map(serializer.serialize)
            }
        }.flatMap(r => r).recover { case t: Throwable =>
            // If any exception occurred so far, serialize it. Note that it may even serialize exception that occurred
            // during previous serialization of successful result. Wrap all exceptions that aren't the RpcException
            // to an RpcException so the client can easily determine, that any kind of exception occurred.
            val rpcException = t match {
                case r: RpcException => r
                case _ => {
                    val cause = Some(throwableToCause(t))
                    new RpcException(s"An exception occurred during invocation of '$methodIdentifier'.", cause)
                }
            }

            // The RpcException is always serializable, so it's sure the following serialization won't fail.
            serializer.serialize(rpcException)
        }
    }

    /**
     * Converts the method identifier to the target object and method symbol. If the object doesn't exist, the method
     * isn't invokable or doesn't return a Future, then throws an exception.
     */
    private def processMethodIdentifier(methodIdentifier: String): (Any, MethodSymbol) = {
        if (methodIdentifier == null || methodIdentifier == "") {
            throw new RpcException("The method identifier must be non-empty.")
        }
        val index = methodIdentifier.lastIndexOf(".")
        if (index <= 0 || index >= (methodIdentifier.length - 1)) {
            throw new RpcException(s"The method identifier '$methodIdentifier' is invalid.")
        }

        // The invocation target.
        val objectTypeFullName = methodIdentifier.take(index)
        val target = cache.getObjectSymbol(objectTypeFullName)

        // The method to invoke.
        val methodName = methodIdentifier.drop(index + 1)
        val method = target.typeSignature.member(newTermName(methodName)) match {
            case m: MethodSymbol => m
            case NoSymbol => throw new RpcException(s"The '$objectTypeFullName' doesn't contain method '$methodName'.")
        }

        // Check whether the method is actually a remote method.
        def hasRemoteAnnotation(symbol: Symbol): Boolean = symbol.annotations.exists(_.tpe =:= typeOf[swat.remote])
        if (!hasRemoteAnnotation(target) && !hasRemoteAnnotation(method)) {
            throw new RpcException(s"The method '${method.fullName}' has to be annotated with @swat.remote.")
        }

        // Check whether the method returns a future.
        if (!(method.returnType <:< typeOf[Future[_]])) {
            throw new RpcException(s"The method '${method.fullName}' has to return subtype of Future[_].")
        }

        (cache.getObject(objectTypeFullName), method)
    }

    /** Deserializes the remote method arguments stored in a tuple. */
    private def deserializeArguments(arguments: String, method: MethodSymbol): List[Any] = {
        val argumentTypes = method.paramss.flatten.toList.map(_.typeSignature)
        if (argumentTypes.nonEmpty) {
            // Turn the method parameter types to a tuple of the types
            val genericTupleType = cache.tupleTypes(argumentTypes.length)
            val tupleType = appliedType(genericTupleType, argumentTypes)

            // Deserialize the arguments as a tuple and return items of the tuple.
            val argumentTuple = serializer.deserialize(arguments, Some(tupleType))
            argumentTuple.asInstanceOf[Product].productIterator.toList
        } else {
            Nil
        }
    }

    /** Converts the specified throwable (and the nested throwables) to a [[swat.common.rpc.Cause]]. */
    private def throwableToCause(t: Throwable): Cause = {
        val cause = Option(t.getCause).map(throwableToCause)
        Cause(t.getClass.getName, t.getMessage, t.getStackTrace.mkString("\n"), cause)
    }
}
