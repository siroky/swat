/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2002-2013, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// DO NOT EDIT, CHANGES WILL BE LOST.

package swat.scala

import scala.language.implicitConversions



object Long extends AnyValCompanion {
  /** The smallest value representable as a Long.
   */
  final val MinValue = java.lang.Long.MIN_VALUE

  /** The largest value representable as a Long.
   */
  final val MaxValue = java.lang.Long.MAX_VALUE

  /** Transform a value type into a boxed reference type.
   *
   *  @param  x   the Long to be boxed
   *  @return     a java.lang.Long offering `x` as its underlying value.
   */
  def box(x: Long): java.lang.Long = java.lang.Long.valueOf(x)

  /** Transform a boxed type into a value type.  Note that this
   *  method is not typesafe: it accepts any Object, but will throw
   *  an exception if the argument is not a java.lang.Long.
   *
   *  @param  x   the java.lang.Long to be unboxed.
   *  @throws     ClassCastException  if the argument is not a java.lang.Long
   *  @return     the Long resulting from calling longValue() on `x`
   */
  def unbox(x: java.lang.Object): Long = x.asInstanceOf[java.lang.Long].longValue()

  /** The String representation of the scala.Long companion object.
   */
  override def toString = "object scala.Long"

  /** Language mandated coercions from Long to "wider" types.
   */
  implicit def long2float(x: Long): Float = x.toFloat
  implicit def long2double(x: Long): Double = x.toDouble
}

