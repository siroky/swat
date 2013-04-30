package swat.internal.scala.concurrent

import scala.annotation.implicitNotFound

@implicitNotFound("Don't call `Awaitable` methods directly, use the `Await` object.")
sealed trait CanAwait
