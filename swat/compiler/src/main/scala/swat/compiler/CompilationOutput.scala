package swat.compiler

case class CompilationOutput(classOutputs: Map[String, js.Program], warnings: Seq[String], infos: Seq[String])
