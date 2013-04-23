package swat.js

import swat.js.applications.{Console, Window, Json}
import swat.js.html.Document
import swat.js.workers.{SharedWorkerGlobalScope, DedicatedWorkerGlobalScope}

trait Scope {
    val JSON: Json = ???

    val Infinity: Any = ???
    val NaN: Any = ???
    val undefined: Any = ???

    def decodeURI(uri: String): String = ???
    def decodeURIComponent(uri: String): String = ???
    def encodeURI(uri: String): String = ???
    def encodeURIComponent(uri: String): String = ???
    def escape(s: String): String = ???
    def eval(js: String): Any = ???
    def isFinite(value: Any): Boolean = ???
    def isNaN(value: Any): Boolean = ???
    def parseFloat(value: Any): Double = ???
    def parseInt(value: Any): Int = ???
    def unescape(s: String): String = ???
}

/**
 * A scope object that may be used anywhere. Contains only field and methods that can be accessed in all possible
 * JavaScript scopes (normal script scope, dedicated worker scope, shared worker scope). Therefore any class may rely
 * on the [[swat.js.CommonScope]].
 */
object CommonScope extends Scope

/**
 * A scope object that may be used only in the standard script execution context. If a class relies on anything that
 * is in the [[swat.js.DefaultScope]] but not in the [[swat.js.CommonScope]], then the class is usable only in
 * that standard script execution context (e.g. it's not usable in a worker context).
 * @note Always prefer to use the [[swat.js.CommonScope]] so the classes that depend on its fields are usable in the
 *       widest range of execution contexts.
 */
object DefaultScope extends Scope {
    val window: Window = ???
    val document: Document = ???
    val console: Console = ???
}

/**
 * A scope object that may be usable only in the dedicated worker execution context.
 * @note Always prefer to use the [[swat.js.CommonScope]] so the classes that depend on its fields are usable in the
 *       widest range of execution contexts.
 */
object DedicatedWorkerScope extends DedicatedWorkerGlobalScope

/**
 * A scope object that may be usable only in the shared worker execution context.
 * @note Always prefer to use the [[swat.js.CommonScope]] so the classes that depend on its fields are usable in the
 *       widest range of execution contexts.
 */
object SharedWorkerScope extends SharedWorkerGlobalScope
