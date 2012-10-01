package swat.compiler.frontend

sealed abstract class ArtifactType

case object ClassArtifact extends ArtifactType
case object TraitArtifact extends ArtifactType
case object ObjectArtifact extends ArtifactType
case object PackageObjectArtifact extends ArtifactType
