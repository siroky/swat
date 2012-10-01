package swat.compiler

case class CompilationOutput(definitionOutputs: Map[String, js.Program], warnings: Seq[String], infos: Seq[String])
