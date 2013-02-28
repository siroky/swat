package swat.api.adapters.events

trait UIEvent[+A <: EventTarget] extends Event[A] {
    val detail: Int
}
