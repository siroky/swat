package swat.api

import annotation.StaticAnnotation

/**
 * An annotation that makes the Swat compiler ignore the annotated method, field, class or an object. So the annotated
 * symbol isn't compiled into JavaScript. In case the annotated symbol is used within code, that is compiled into
 * JavaScript, a compilation error is triggered.
 */
class ignored extends StaticAnnotation
