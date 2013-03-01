package swat.api.js.events

trait EventListener {
    def handleEvent(event: Event[_ <: EventTarget])
}
