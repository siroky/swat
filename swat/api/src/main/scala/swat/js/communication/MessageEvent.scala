package swat.js.communication

import swat.js
import swat.js.events.{Event, EventTarget}

trait MessageEvent[+A <: EventTarget] extends Event[A] {
    val data: Any = ???
    val origin: String = ???
    val lastEventId: String = ???
    val source: Any = ???
    val ports: js.Array[MessagePort] = ???
}
