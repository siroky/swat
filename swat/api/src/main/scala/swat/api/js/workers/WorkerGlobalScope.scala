package swat.api.js.workers

import swat.api.js.events.{Event, EventTarget}
import swat.api.js.applications.{WindowBase64, WindowTimers}
import swat.api.js.Scope

trait WorkerGlobalScope extends Scope with EventTarget with WindowTimers with WindowBase64 {
    val self: WorkerGlobalScope = ???
    val location: WorkerLocation = ???
    val navigator: WorkerNavigator = ???
    var onerror: Event[this.type] => Unit = ???
    var onoffline: Event[this.type] => Unit = ???
    var ononline: Event[this.type] => Unit = ???

    def close() {}
    def importScripts(url: String) {}
}
