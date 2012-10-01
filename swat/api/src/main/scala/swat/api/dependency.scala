package swat.api

import annotation.StaticAnnotation
import reflect.runtime.universe.Type

/**
 * Can be used in combination with the [[swat.api.native]] annotation to specify classes or objects that are used
 * in the native JavaScript code, so they are recognized as dependencies of the annotated symbol.
 * @param tpe Type of the dependency, e.g. typeOf[scala.Some].
 * @param isHard Whether the dependency is hard (the dependency has to be declared before the annotated symbol) or
 *               soft (declaration order of the dependency and the annotated symbol doesn't matter).
 */
class dependency(tpe: Type, isHard: Boolean = true) extends StaticAnnotation
