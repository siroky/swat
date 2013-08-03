package swat.js.events

trait EventListener {
    def handleEvent(event: Event[_ <: EventTarget])
}
