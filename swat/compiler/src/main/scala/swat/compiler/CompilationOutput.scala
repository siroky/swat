package swat.compiler

case class CompilationOutput(program: js.Program, warnings: Seq[String], infos: Seq[String])
