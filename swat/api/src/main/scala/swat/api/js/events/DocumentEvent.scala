package swat.api.js.events

trait DocumentEvent {
    def createEvent[A <: Event[_ <: EventTarget]](eventInterface: String): A = ???
}
