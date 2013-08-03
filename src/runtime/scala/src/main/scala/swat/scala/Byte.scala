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



object Byte extends AnyValCompanion {
  /** The smallest value representable as a Byte.
   */
  final val MinValue = java.lang.Byte.MIN_VALUE

  /** The largest value representable as a Byte.
   */
  final val MaxValue = java.lang.Byte.MAX_VALUE

  /** Transform a value type into a boxed reference type.
   *
   *  @param  x   the Byte to be boxed
   *  @return     a java.lang.Byte offering `x` as its underlying value.
   */
  def box(x: Byte): java.lang.Byte = java.lang.Byte.valueOf(x)

  /** Transform a boxed type into a value type.  Note that this
   *  method is not typesafe: it accepts any Object, but will throw
   *  an exception if the argument is not a java.lang.Byte.
   *
   *  @param  x   the java.lang.Byte to be unboxed.
   *  @throws     ClassCastException  if the argument is not a java.lang.Byte
   *  @return     the Byte resulting from calling byteValue() on `x`
   */
  def unbox(x: java.lang.Object): Byte = x.asInstanceOf[java.lang.Byte].byteValue()

  /** The String representation of the scala.Byte companion object.
   */
  override def toString = "object scala.Byte"

  /** Language mandated coercions from Byte to "wider" types.
   */
  implicit def byte2short(x: Byte): Short = x.toShort
  implicit def byte2int(x: Byte): Int = x.toInt
  implicit def byte2long(x: Byte): Long = x.toLong
  implicit def byte2float(x: Byte): Float = x.toFloat
  implicit def byte2double(x: Byte): Double = x.toDouble
}

