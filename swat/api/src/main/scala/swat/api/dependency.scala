package swat.api

import annotation.StaticAnnotation

/**
 * Can be used in combination with the [[swat.api.native]] annotation to specify classes or objects that are used
 * in the native JavaScript code, so they are recognized as dependencies of the annotated symbol.
 * @param cls Class of the dependency, e.g. classOf[String].
 * @param isHard Whether the dependency is hard (the dependency has to be declared before the annotated symbol) or
 *               soft (declaration order of the dependency and the annotated symbol doesn't matter).
 */
class dependency(cls: Class[_], isHard: Boolean = true) extends StaticAnnotation
