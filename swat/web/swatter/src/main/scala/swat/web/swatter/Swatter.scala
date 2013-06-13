package swat.web.swatter

import swat.adapter

object Application extends App {
    val scalaEditor = ace.edit("scala-editor")
    scalaEditor.setTheme("ace/theme/github")
    scalaEditor.getSession.setMode("ace/mode/scala")

    val javaScriptEditor = ace.edit("javascript-editor")
    javaScriptEditor.setTheme("ace/theme/github")
    javaScriptEditor.getSession.setMode("ace/mode/javascript")
}

@adapter object ace {
    def edit(id: String): Ace = ???
}

@adapter trait Ace {
    def setValue(value: String) {}
    def getValue: String = ???
    def setTheme(theme: String) {}
    def getSession: AceSession = ???
}

@adapter trait AceSession {
    def setMode(mode: String) {}
}
