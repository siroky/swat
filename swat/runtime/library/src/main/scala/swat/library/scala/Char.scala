/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2002-2013, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// DO NOT EDIT, CHANGES WILL BE LOST.

package swat.library.scala

import scala.language.implicitConversions



object Char extends AnyValCompanion {
  /** The smallest value representable as a Char.
   */
  final val MinValue = java.lang.Character.MIN_VALUE

  /** The largest value representable as a Char.
   */
  final val MaxValue = java.lang.Character.MAX_VALUE

  /** Transform a value type into a boxed reference type.
   *
   *  @param  x   the Char to be boxed
   *  @return     a java.lang.Character offering `x` as its underlying value.
   */
  def box(x: Char): java.lang.Character = java.lang.Character.valueOf(x)

  /** Transform a boxed type into a value type.  Note that this
   *  method is not typesafe: it accepts any Object, but will throw
   *  an exception if the argument is not a java.lang.Character.
   *
   *  @param  x   the java.lang.Character to be unboxed.
   *  @throws     ClassCastException  if the argument is not a java.lang.Character
   *  @return     the Char resulting from calling charValue() on `x`
   */
  def unbox(x: java.lang.Object): Char = x.asInstanceOf[java.lang.Character].charValue()

  /** The String representation of the scala.Char companion object.
   */
  override def toString = "object scala.Char"

  /** Language mandated coercions from Char to "wider" types.
   */
  implicit def char2int(x: Char): Int = x.toInt
  implicit def char2long(x: Char): Long = x.toLong
  implicit def char2float(x: Char): Float = x.toFloat
  implicit def char2double(x: Char): Double = x.toDouble
}

