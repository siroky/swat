package swat.api.js.workers

import swat.api.js.events.{Event, EventTarget}

trait ErrorEvent[+A <: EventTarget] extends Event[A] {
    val message: String = ???
    val filename: String = ???
    val lineno: Long = ???
    val column: Long = ???
}
