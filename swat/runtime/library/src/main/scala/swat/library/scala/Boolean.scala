/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2002-2013, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// DO NOT EDIT, CHANGES WILL BE LOST.

package swat.library.scala



object Boolean extends AnyValCompanion {

  /** Transform a value type into a boxed reference type.
   *
   *  @param  x   the Boolean to be boxed
   *  @return     a java.lang.Boolean offering `x` as its underlying value.
   */
  def box(x: Boolean): java.lang.Boolean = java.lang.Boolean.valueOf(x)

  /** Transform a boxed type into a value type.  Note that this
   *  method is not typesafe: it accepts any Object, but will throw
   *  an exception if the argument is not a java.lang.Boolean.
   *
   *  @param  x   the java.lang.Boolean to be unboxed.
   *  @throws     ClassCastException  if the argument is not a java.lang.Boolean
   *  @return     the Boolean resulting from calling booleanValue() on `x`
   */
  def unbox(x: java.lang.Object): Boolean = x.asInstanceOf[java.lang.Boolean].booleanValue()

  /** The String representation of the scala.Boolean companion object.
   */
  override def toString = "object scala.Boolean"

}

