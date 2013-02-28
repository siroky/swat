package swat.api.adapters.events

trait CustomEvent[+A <: EventTarget] extends Event[A] {
    val detail: Any
}
