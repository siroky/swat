package swat.js.workers

import swat.js.events.{Event, EventTarget}
import swat.js.applications.{WindowBase64, WindowTimers}
import swat.js.Scope

trait WorkerGlobalScope extends Scope with EventTarget with WindowTimers with WindowBase64 {
    val self: this.type = ???
    val location: WorkerLocation = ???
    val navigator: WorkerNavigator = ???
    var onerror: Event[this.type] => Unit = ???
    var onoffline: Event[this.type] => Unit = ???
    var ononline: Event[this.type] => Unit = ???

    def close() {}
    def importScripts(url: String) {}
}
