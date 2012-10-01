package swat.compiler.frontend

sealed abstract class DefinitionType

case object ClassDefinition extends DefinitionType
case object TraitDefinition extends DefinitionType
case object ObjectDefinition extends DefinitionType
case object PackageObjectDefinition extends DefinitionType
