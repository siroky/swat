package swat.js.applications

import swat.js.html._
import swat.js.events._

trait Window extends EventTarget with WindowTimers with WindowBase64 {
    val closed: Boolean = ???
    var defaultStatus: String = ???
    val document: Document = ???
    val history: History = ???
    var innerHeight: Double = ???
    var innerWidth: Double = ???
    val location: Location = ???
    var name: String = ???
    val navigator: Navigator = ???
    val opener: Window = ???
    var outerHeight: Double = ???
    var outerWidth: Double = ???
    val pageXOffset: Double = ???
    val pageYOffset: Double = ???
    val parent: Window = ???
    val screen: Screen = ???
    val screenLeft: Double = ???
    var screenTop: Double = ???
    val screenX: Double = ???
    val screenY: Double = ???
    val self: Window = ???
    var status: String = ???
    val top: Window = ???
    val sessionStorage: Storage = ???
    val localStorage: Storage = ???
    var onresize: Event[this.type] => Unit = ???
    var onunload: Event[this.type] => Unit = ???

    def get(key: String) {}
    def focus() {}
    def alert(s: Any) {}
    def open(url: String) {}
}



