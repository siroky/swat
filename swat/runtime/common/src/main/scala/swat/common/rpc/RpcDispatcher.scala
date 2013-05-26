package swat.common.rpc

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.reflect.runtime.universe._
import swat.common.json.JsonSerializer
import swat.common.reflect.CachedMirror

/**
 * A dispatcher of remote method calls to the singleton objects. Responsible for both deserialization of the method
 * arguments and serialization of the result.
 */
@swat.ignored
class RpcDispatcher {

    val mirror = new CachedMirror
    val serializer = new JsonSerializer(mirror)

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
            // Performing all the operations inside the future so any exception throw either here or inside the invoked
            // method can be finally processed in the recover block.
            val (target, method) = processMethodIdentifier(methodIdentifier)
            val arguments = deserializeArguments(serializedArguments, method)
            val methodMirror = mirror.use(_.reflect(target)).reflectMethod(method)

            // Invoke the method and serialize the result
            val result = methodMirror(arguments: _*).asInstanceOf[Future[Any]]
            result.map(serializer.serialize(_))

        }.flatMap(r => r).recover {
            // If any exception occurred so far, serialize it. The serializer should always serialize a Throwable
            // without any exceptions (Note that it may even serialize exception that occurred during previous
            // serialization of successful result).
            case e: RpcException => serializer.serialize(e)
            case t: Throwable => serializer.serialize(new RpcException(t.getMessage))
        }
    }

    /**
     * Converts the method identifier to the target object and method symbol. If the object doesn't exist, the method
     * isn't invokable or doesn't return a Future, then throws an exception.
     */
    def processMethodIdentifier(methodIdentifier: String): (Any, MethodSymbol) = {
        if (methodIdentifier == null || methodIdentifier == "") {
            throw new RpcException("The method identifier must be non-empty.")
        }
        val index = methodIdentifier.lastIndexOf(".")
        if (index <= 0 || index >= (methodIdentifier.length - 1)) {
            throw new RpcException(s"The method identifier '$methodIdentifier' is invalid.")
        }

        // The invocation target.
        val objectTypeFullName = methodIdentifier.take(index)
        val target = mirror.getObjectSymbol(objectTypeFullName)

        // The method to invoke.
        val methodName = methodIdentifier.drop(index + 1)
        val method = target.typeSignature.member(newTermName(methodName)) match {
            case m: MethodSymbol => m
            case NoSymbol => throw new RpcException(s"The '$objectTypeFullName' doesn't contain method '$methodName'.")
        }

        // Check whether the method is actually a remote method.
        val remoteAnnotations = List(target, method).flatMap(_.annotations.filter(_.tpe =:= typeOf[swat.remote]))
        if (remoteAnnotations.isEmpty) {
            throw new RpcException(s"The method '${method.fullName}' has to be annotated with @swat.remote.")
        }

        // Check whether the method returns a future.
        if (!(method.returnType <:< typeOf[Future[_]])) {
            throw new RpcException(s"The method '${method.fullName}' has to return subtype of Future[_].")
        }

        (mirror.getObject(objectTypeFullName), method)
    }

    /** Deserializes the remote method arguments stored in a tuple. */
    def deserializeArguments(arguments: String, method: MethodSymbol): List[Any] = {
        val argumentTypes = method.paramss.flatten.toList.map(_.typeSignature)
        if (argumentTypes.nonEmpty) {
            // Turn the method parameter types to a tuple of the types
            val genericTupleType = mirror.tupleTypes(argumentTypes.length)
            val tupleType = appliedType(genericTupleType, argumentTypes)

            // Deserialize the arguments as a tuple and return items of the tuple.
            val argumentTuple = serializer.deserialize(arguments, Some(tupleType))
            argumentTuple.asInstanceOf[Product].productIterator.toList
        } else {
            Nil
        }
    }
}
