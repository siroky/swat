package swat

import annotation.StaticAnnotation

/**
 * An annotation for marking public methods that can be invoked from the client side in an RPC fashion. The limitation
 * is, that the method must return a [[scala.concurrent.Future]]. If all methods of an object should be remote, then
 * it suffices to mark only the object remote. And all its public methods become remote.
 *
 * The whole process roughly works as follows:
 *     1) Instead of the method invocation, swat.invokeRemote is invoked.
 *     2) It serializes the method arguments and sends them together with the object type name to the server via AJAX.
 *     3) The server deserializes the parameters and invokes he method through reflection.
 *     4) The server serializes the result and sends it as a response to the request from the client.
 *     5) The result is deserialized and returned.
 */
class remote extends StaticAnnotation
