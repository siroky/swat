/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2013, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package swat.scala.concurrent.impl



import scala.concurrent.ExecutionContext
import swat.scala.util.control.NonFatal
import swat.scala.util.{ Success, Failure }


private[concurrent] object Future {
  /* Swat modified
  class PromiseCompletingRunnable[T](body: => T) extends Runnable {
    val promise = new Promise.DefaultPromise[T]()

    override def run() = {
      promise complete {
        try Success(body) catch { case NonFatal(e) => Failure(e) }
      }
    }
  }

  def apply[T](body: =>T)(implicit executor: ExecutionContext): scala.concurrent.Future[T] = {
    val runnable = new PromiseCompletingRunnable(body)
    executor.execute(runnable)
    runnable.promise.future
  }*/

  def apply[T](body: => T)(implicit executor: swat.scala.concurrent.ExecutionContext): swat.scala.concurrent.Future[T] = {
    val promise = new Promise.DefaultPromise[T]()
    promise complete {
      try Success(body) catch { case NonFatal(e) => Failure(e) }
    }
    promise.future
  }
}
