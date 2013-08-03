package swat.js.events

trait CompositionEvent[+A <: EventTarget] extends UIEvent[A] {
    val data: String
    val locale: String
}
