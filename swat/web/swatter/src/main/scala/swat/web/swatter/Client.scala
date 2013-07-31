package swat.web.swatter

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import swat.js.DefaultScope._
import swat.adapter
import swat.common.rpc.{Cause, RpcException}
import swat.js.jquery.jQuery

object Client extends App {

    val testAppName = "TestApp"
    var currentCodePackage: Option[CodePackage] = None

    val scalaEditor = ace.edit("scala-editor")
    val javaScriptEditor = ace.edit("javascript-editor")
    val compileButton = document.getElementById("compile-button")
    val runButton = document.getElementById("run-button")
    val compilingModal = jQuery("#compiling-modal")

    scalaEditor.setTheme("ace/theme/github")
    scalaEditor.getSession.setMode("ace/mode/scala")
    javaScriptEditor.setTheme("ace/theme/github")
    javaScriptEditor.getSession.setMode("ace/mode/javascript")
    scalaEditor.setValue(
        "import swat.js.DefaultScope._\n" +
        "\n" +
        "// Do not change the application name if you want it to be runnable.\n" +
        "object " + testAppName + " extends App {\n" +
        "    writeToBody(\"Going to greet.\")\n" +
        "    window.alert(\"Hello World from Swat.\")\n" +
        "    writeToBody(\"Greeted.\")\n" +
        "\n" +
        "    def writeToBody(text: String) {\n" +
        "        document.body.appendChild(document.createTextNode(text))\n" +
        "    }\n" +
        "}")
    scalaEditor.selection.clearSelection()
    updateControls()

    compileButton.onclick = e => {
        compilingModal.modal("show")
        Server.compile(scalaEditor.getValue).onComplete { result =>
            currentCodePackage = result.toOption
            updateControls()

            val value = result match {
                case Success(codePackage) => codePackage.compiledCode
                case Failure(t: Throwable) => t.toString
            }
            javaScriptEditor.setValue(value)
            javaScriptEditor.selection.clearSelection()
            compilingModal.modal("hide")
        }
    }

    runButton.onclick = e => {
        val codeWindow = window.open("", "_blank")
        codeWindow.eval(currentCodePackage.get.runnableCode + "\n" + testAppName + "$();")
    }

    private def updateControls() {
        if (currentCodePackage.isEmpty) {
            runButton.setAttribute("disabled", "")
        } else {
            runButton.removeAttribute("disabled")
        }
    }
}

@adapter object ace {
    def edit(id: String): Ace = ???
}

@adapter trait Ace {
    val session: AceSession = ???
    val selection: AceSelection = ???

    def setValue(value: String) {}
    def getValue: String = ???
    def setTheme(theme: String) {}
    def getSession: AceSession = ???
}

@adapter trait AceSession {
    def setMode(mode: String) {}
}

@adapter trait AceSelection {
    def clearSelection() {}
}
