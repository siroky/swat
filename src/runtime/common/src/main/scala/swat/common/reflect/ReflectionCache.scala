package swat.common.reflect

import scala.reflect.runtime.universe._
import scala.collection.mutable

/** Cache around a reflection mirror so same operations performed multiple times are actually done only once. */
@swat.ignored
class ReflectionCache(val mirror: Mirror = runtimeMirror(getClass.getClassLoader)) {

    /** Class to symbol cache. */
    private val classSymbols = mutable.HashMap[String, ClassSymbol]()
    private val instanceSymbols = mutable.HashMap[Class[_], ClassSymbol]()
    private val objectSymbols = mutable.HashMap[String, ModuleSymbol]()
    private val objects = mutable.HashMap[String, Any]()

    /** Returns symbol with the specified type name. */
    def getClassSymbol(fullName: String): ClassSymbol = mirror.synchronized {
        classSymbols.getOrElseUpdate(fullName, mirror.classSymbol(Class.forName(fullName)))
    }

    /** Returns class symbol of the specified instance. */
    def getInstanceSymbol(instance: Any): ClassSymbol = mirror.synchronized {
        val c = instance.getClass
        instanceSymbols.getOrElseUpdate(c, mirror.classSymbol(c))
    }

    /** Returns symbol of the singleton object with the specified name. */
    def getObjectSymbol(fullName: String): ModuleSymbol = mirror.synchronized {
        objectSymbols.getOrElseUpdate(fullName, mirror.moduleSymbol(Class.forName(fullName)))
    }

    /** Returns singleton object with the specified name. */
    def getObject(fullName: String): Any = {
        objects.getOrElseUpdate(fullName, {
            // TODO find out why the following doesn't work: mirror.reflectModule(getObjectSymbol(fullName))).instance
            val c = Class.forName(fullName.stripSuffix("$") + "$")
            c.getField("MODULE$").get(c)
        })
    }

    /**
     * Types of tuples by their arity.
     * TODO find out why the following doesn't work instead: mirror.getClassSymbol("scala.Tuple" + n).typeSignature
     */
    lazy val tupleTypes = Map(
        1 -> typeOf[Tuple1[_]],
        2 -> typeOf[(_, _)],
        3 -> typeOf[(_, _, _)],
        4 -> typeOf[(_, _, _, _)],
        5 -> typeOf[(_, _, _, _, _)],
        6 -> typeOf[(_, _, _, _, _, _)],
        7 -> typeOf[(_, _, _, _, _, _, _)],
        8 -> typeOf[(_, _, _, _, _, _, _, _)],
        9 -> typeOf[(_, _, _, _, _, _, _, _, _)],
        10 -> typeOf[(_, _, _, _, _, _, _, _, _, _)],
        11 -> typeOf[(_, _, _, _, _, _, _, _, _, _, _)],
        12 -> typeOf[(_, _, _, _, _, _, _, _, _, _, _, _)],
        13 -> typeOf[(_, _, _, _, _, _, _, _, _, _, _, _, _)],
        14 -> typeOf[(_, _, _, _, _, _, _, _, _, _, _, _, _, _)],
        15 -> typeOf[(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _)],
        16 -> typeOf[(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)],
        17 -> typeOf[(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)],
        18 -> typeOf[(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)],
        19 -> typeOf[(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)],
        20 -> typeOf[(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)],
        21 -> typeOf[(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)],
        22 -> typeOf[(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)]
    )
}
