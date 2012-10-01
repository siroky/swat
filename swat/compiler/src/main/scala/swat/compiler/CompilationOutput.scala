package swat.compiler

import frontend.ArtifactRef

case class CompilationOutput(artifactOutputs: Map[ArtifactRef, js.Program], warnings: Seq[String], infos: Seq[String])
