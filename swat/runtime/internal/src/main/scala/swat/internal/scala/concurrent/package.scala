/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2013, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package swat.internal.scala

import scala.concurrent.duration.Duration
import scala.annotation.implicitNotFound

/** This package object contains primitives for concurrent and parallel programming.
 */
package object concurrent {
  type ExecutionException =    java.util.concurrent.ExecutionException
  type CancellationException = java.util.concurrent.CancellationException
  type TimeoutException =      java.util.concurrent.TimeoutException

  /** Starts an asynchronous computation and returns a `Future` object with the result of that computation.
   *  
   *  The result becomes available once the asynchronous computation is completed.
   *  
   *  @tparam T       the type of the result
   *  @param body     the asynchronous computation
   *  @return         the `Future` holding the result of the computation
   */
  def future[T](body: =>T)(implicit execctx: ExecutionContext): Future[T] = Future[T](body)

  /** Creates a promise object which can be completed with a value or an exception.
   *  
   *  @tparam T       the type of the value in the promise
   *  @return         the newly created `Promise` object
   */
  def promise[T](): Promise[T] = Promise[T]()
}
