package swat.api.adapters.events

trait EventListener {
    def handleEvent(event: Event[_ <: EventTarget])
}
