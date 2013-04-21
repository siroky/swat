package swat.js.events

trait CustomEvent[+A <: EventTarget] extends Event[A] {
    val detail: Any
}
