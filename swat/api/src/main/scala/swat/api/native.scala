package swat.api

import annotation.StaticAnnotation

/**
 * An annotation that makes the Swat compiler replace the body of the annotated symbol with the specified JavaScript
 * code. If it annotates a method, the javaScriptCode should be just the method body. If it annotates a field, then
 * the javaScriptCode should be the right side of the field initialization assignment. If it annotates a class or an
 * object, then the javaScriptCode is used instead of the class declaration, i.e. the compiler doesn't produce
 * anything related to the class, the only output of it is the javaScriptCode.
 * @param javaScriptCode The native JavaScript code.
 * @param dependencies If the javaScriptCode contains references to some other classes or objects, that aren't part
 *                     of the file, then full class names of the referenced symbols have to be included into the
 *                     dependency list.
 */
class native(val javaScriptCode: String, val dependencies: Traversable[String] = Nil) extends StaticAnnotation
