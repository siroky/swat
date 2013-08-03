package swat.js.workers

import swat.js.events.{Event, EventTarget}

trait ErrorEvent[+A <: EventTarget] extends Event[A] {
    val message: String = ???
    val filename: String = ???
    val lineno: Long = ???
    val column: Long = ???
}
