package swat.web.swatter

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import swat.js.DefaultScope._
import swat.{adapter, remote}
import swat.compiler.SwatCompiler
import swat.compiler.backend.JsCodeGenerator
import swat.common.rpc.RpcException

object Application extends App {

    val scalaEditor = ace.edit("scala-editor")
    val javaScriptEditor = ace.edit("javascript-editor")
    val compileButton = document.getElementById("compile-button")

    scalaEditor.setTheme("ace/theme/github")
    scalaEditor.getSession.setMode("ace/mode/scala")
    javaScriptEditor.setTheme("ace/theme/github")
    javaScriptEditor.getSession.setMode("ace/mode/javascript")

    compileButton.onclick = { e =>
        Compiler.compile(scalaEditor.getValue).onComplete {
            case Success(javaScriptCode) => javaScriptEditor.setValue(javaScriptCode)
            case Failure(r: RpcException) => javaScriptEditor.setValue(r.message)
        }
    }
}

@remote object Compiler {
    def compile(scalaCode: String): Future[String] = future {
        val compiler = new SwatCompiler(None, None, None)
        val compilationOutput = compiler.compile(scalaCode)
        val jsPrograms = compilationOutput.typeOutputs.values
        val codeGenerator = new JsCodeGenerator
        jsPrograms.map(codeGenerator.astToCode(_)).mkString("\n\n")
    }
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
