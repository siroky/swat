package swat.compiler.frontend

sealed abstract class ClassSymbolKind

case object ClassSymbol extends ClassSymbolKind
case object TraitSymbol extends ClassSymbolKind
case object ObjectSymbol extends ClassSymbolKind
case object PackageObjectSymbol extends ClassSymbolKind
