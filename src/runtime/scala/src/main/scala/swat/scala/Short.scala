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



object Short extends AnyValCompanion {
  /** The smallest value representable as a Short.
   */
  final val MinValue = java.lang.Short.MIN_VALUE

  /** The largest value representable as a Short.
   */
  final val MaxValue = java.lang.Short.MAX_VALUE

  /** Transform a value type into a boxed reference type.
   *
   *  @param  x   the Short to be boxed
   *  @return     a java.lang.Short offering `x` as its underlying value.
   */
  def box(x: Short): java.lang.Short = java.lang.Short.valueOf(x)

  /** Transform a boxed type into a value type.  Note that this
   *  method is not typesafe: it accepts any Object, but will throw
   *  an exception if the argument is not a java.lang.Short.
   *
   *  @param  x   the java.lang.Short to be unboxed.
   *  @throws     ClassCastException  if the argument is not a java.lang.Short
   *  @return     the Short resulting from calling shortValue() on `x`
   */
  def unbox(x: java.lang.Object): Short = x.asInstanceOf[java.lang.Short].shortValue()

  /** The String representation of the scala.Short companion object.
   */
  override def toString = "object scala.Short"

  /** Language mandated coercions from Short to "wider" types.
   */
  implicit def short2int(x: Short): Int = x.toInt
  implicit def short2long(x: Short): Long = x.toLong
  implicit def short2float(x: Short): Float = x.toFloat
  implicit def short2double(x: Short): Double = x.toDouble
}

