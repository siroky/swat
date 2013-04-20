package swat.api.js

import swat.api.js.applications.{Console, Window}
import swat.api.js.html.Document

object GlobalScope extends Scope {
    val window: Window = ???
    val document: Document = ???
    val console: Console = ???
}
