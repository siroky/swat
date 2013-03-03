package swat.api

import annotation.StaticAnnotation

/**
 * An annotation that should be used to mark classes or objects that represent existing JavaScript classes or objects.
 * They're not compiled into JavaScript, however when they're used within code, that is compiled into JavaScript, names
 * of the annotated symbols and names of methods encapsulated within them aren't changed in any way. The main purpose
 * of this annotation is to provide a simple way how to integrate existing libraries written in JavaScript with Scala
 * code, that is processed by the Swat compiler.
 * @param stripPackage If true, not only the package containing adapters is stripped, but also subpacakages are
 *                     stripped. E.g. an adapter: my.adapters.foo.bar.Adapter where the my.adapters is an adapter
 *                     package. If true, then the resulting name is Adapter. If false, then it's foo.bar.Adapter.
 */
class adapter(stripPackage: Boolean = true) extends StaticAnnotation
