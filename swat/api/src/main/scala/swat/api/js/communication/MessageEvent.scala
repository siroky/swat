package swat.api.js.communication

import swat.api.js
import swat.api.js.events.{Event, EventTarget}

trait MessageEvent[+A <: EventTarget] extends Event[A] {
    val data: Any = ???
    val origin: String = ???
    val lastEventId: String = ???
    val source: Any = ???
    val ports: js.Array[MessagePort] = ???
}
