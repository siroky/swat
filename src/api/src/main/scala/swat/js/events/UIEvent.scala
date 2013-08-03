package swat.js.events

trait UIEvent[+A <: EventTarget] extends Event[A] {
    val detail: Int
}
