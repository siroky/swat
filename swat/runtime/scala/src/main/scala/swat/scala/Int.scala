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



object Int extends AnyValCompanion {
  /** The smallest value representable as a Int.
   */
  final val MinValue = java.lang.Integer.MIN_VALUE

  /** The largest value representable as a Int.
   */
  final val MaxValue = java.lang.Integer.MAX_VALUE

  /** Transform a value type into a boxed reference type.
   *
   *  @param  x   the Int to be boxed
   *  @return     a java.lang.Integer offering `x` as its underlying value.
   */
  def box(x: Int): java.lang.Integer = java.lang.Integer.valueOf(x)

  /** Transform a boxed type into a value type.  Note that this
   *  method is not typesafe: it accepts any Object, but will throw
   *  an exception if the argument is not a java.lang.Integer.
   *
   *  @param  x   the java.lang.Integer to be unboxed.
   *  @throws     ClassCastException  if the argument is not a java.lang.Integer
   *  @return     the Int resulting from calling intValue() on `x`
   */
  def unbox(x: java.lang.Object): Int = x.asInstanceOf[java.lang.Integer].intValue()

  /** The String representation of the scala.Int companion object.
   */
  override def toString = "object scala.Int"

  /** Language mandated coercions from Int to "wider" types.
   */
  implicit def int2long(x: Int): Long = x.toLong
  implicit def int2float(x: Int): Float = x.toFloat
  implicit def int2double(x: Int): Double = x.toDouble
}

