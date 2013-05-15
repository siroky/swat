package swat.common.reflect

import scala.reflect.runtime.universe._
import scala.collection.mutable

/**
 * Cached around a reflection mirror so same operations performed multiple times are actually done only once. Also
 * synchronizes access to it, because a reflection mirror isn't currently thread-safe.
 */
@swat.ignored
class CachedMirror(mirror: Mirror = runtimeMirror(getClass.getClassLoader)) {

    /** Class to symbol cache. */
    private val classSymbols = mutable.HashMap[Class[_], ClassSymbol]()

    /** Provides synchronized access to the reflection mirror. */
    def use[T](f: Mirror => T): T = synchronized[T] {
        f(mirror)
    }

    /** Returns symbol corresponding to the specified class. */
    def getClassSymbol(c: Class[_]): ClassSymbol = use(m => classSymbols.getOrElseUpdate(c, m.classSymbol(c)))

    /** Returns symbol with the specified type name. */
    def getClassSymbol(classFullName: String): ClassSymbol = getClassSymbol(Class.forName(classFullName))

    /** Returns class symbol of the specified instance. */
    def getInstanceSymbol(instance: Any): ClassSymbol = getClassSymbol(instance.getClass)

    /** Returns symbol of the singleton object with specified full name. */
    def getObjectSymbol(fullName: String): ModuleSymbol = use(_.staticModule(fullName))

    /** Returns the singleton object with specified full name. */
    def getObject(fullName: String): Any = use(_.reflectModule(getObjectSymbol(fullName)).instance)
}
