package swat.api.js.events

trait FocusEvent[+A <: EventTarget] extends UIEvent[A] {
    val relatedTarget: EventTarget
}
