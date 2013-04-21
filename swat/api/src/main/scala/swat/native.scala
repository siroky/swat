package swat

import annotation.StaticAnnotation

/**
 * An annotation that makes the Swat compiler replace the body of the annotated symbol with the specified JavaScript
 * code. If it annotates a method, the jsCode should be just the method body. If it annotates a field, then the jsCode
 * should be the right side of the field initialization assignment. If it annotates a class or an object, then the
 * jsCode is used instead of the class declaration, i.e. the compiler doesn't produce anything related to the class,
 * the only output of it is the jsCode.
 *
 * If the jsCode contains references to some other classes or objects, use the [[swat.dependency]] to add
 * the used classes as dependencies of the class.
 *
 * @param jsCode The native JavaScript code.
 */
class native(val jsCode: String) extends StaticAnnotation
