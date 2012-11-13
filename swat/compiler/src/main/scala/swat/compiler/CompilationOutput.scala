package swat.compiler

case class CompilationOutput(classOutputs: Map[String, js.Program], warnings: List[String], infos: List[String])
