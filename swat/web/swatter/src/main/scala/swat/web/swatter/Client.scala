package swat.web.swatter

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import swat.js.DefaultScope._
import swat.adapter
import swat.js.jquery.jQuery

/**
 * An example application that utilizes the Swat compiler. When the compile button is clicked, it invokes the
 * [[swat.web.swatter.Server.compile]] method over RPC with the code in the Scala code editor as a parameter.
 * If the compilation succeeds the output is shown in the JavaScript editor. Otherwise the errors are shown there.
 */
object Client extends App {

    var currentCodePackage: Option[CodePackage] = None

    val testAppName = "TestApp"
    val initialCode = """
import swat.js.DefaultScope._

// Do not change the application name if you want it to be runnable.
object """ + testAppName + """ extends App {
    writeToBody("Going to greet.")
    window.alert("Hello World from Swat.")
    writeToBody("Greeted.")

    def writeToBody(text: String) {
        document.body.appendChild(document.createTextNode(text))
    }
}")"""

    // Obtain the components from the web page.
    val scalaEditor = ace.edit("scala-editor")
    val javaScriptEditor = ace.edit("javascript-editor")
    val compileButton = document.getElementById("compile-button")
    val runButton = document.getElementById("run-button")
    val compilingModal = jQuery("#compiling-modal")

    // Initialize the editors.
    scalaEditor.setTheme("ace/theme/github")
    scalaEditor.getSession.setMode("ace/mode/scala")
    javaScriptEditor.setTheme("ace/theme/github")
    javaScriptEditor.getSession.setMode("ace/mode/javascript")
    scalaEditor.setValue(initialCode)
    scalaEditor.selection.clearSelection()
    updateControls()

    // Compile button click handler.
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

    // Run button click handler.
    runButton.onclick = e => {
        val codeWindow = window.open("", "_blank")
        codeWindow.eval(currentCodePackage.get.runnableCode + "\n" + testAppName + "$();")
    }

    // Updates the components according to the current state of compilation.
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
