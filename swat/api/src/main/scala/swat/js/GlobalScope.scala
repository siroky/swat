package swat.js

import swat.js.applications.{Console, Window}
import swat.js.html.Document

object GlobalScope extends Scope {
    val window: Window = ???
    val document: Document = ???
    val console: Console = ???
}
