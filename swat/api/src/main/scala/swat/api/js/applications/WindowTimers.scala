package swat.api.js.applications

trait WindowTimers {
    type Handle

    def setTimeout(fn: () => Unit, milliseconds: Int): Handle = ???
    def clearTimeout(timeoutId: Handle) {}
    def setInterval(fn: () => Unit, milliseconds: Int): Handle = ???
    def clearInterval(intervalId: Handle) {}
}
