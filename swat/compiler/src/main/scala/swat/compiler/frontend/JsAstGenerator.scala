package swat.compiler.frontend

import tools.nsc.Global
import swat.api
import swat.compiler.{CompilationException, js}
import js.ExpressionStatement

trait JsAstGenerator extends js.TreeBuilders
{
    self: {val global: Global} =>

    import global._

    def processCompilationUnit(compilationUnit: CompilationUnit): Map[String, js.Program] = {
        compilationUnit.body match {
            case p: PackageDef => extractClassDefs(p).map(processClassDef _).toMap
            case _ => {
                error("The %s must contain a package definition.".format(compilationUnit.source.file.name))
                Map.empty
            }
        }
    }

    private def extractClassDefs(tree: Tree): List[ClassDef] = tree match {
        case p: PackageDef => p.stats.flatMap(extractClassDefs _)
        case c: ClassDef if c.symbol.isCompiled => c :: c.impl.body.flatMap(extractClassDefs _)
        case _ => Nil
    }

    private def processDependency(dependencyType: Type, isHard: Boolean): js.Statement = {
        ExpressionStatement(js.CallExpression(
            memberSelectionChain("swat", "dependsOn"),
            List(
                js.StringLiteral(dependencyType.typeSymbol.definitionIdentifier),
                js.BooleanLiteral(isHard)
            )
        ))
    }

    private def processClassDef(classDef: ClassDef): (String, js.Program) = {
        val outputProgram = classDef.symbol.nativeAnnotation.map { code =>
            val dependencies = classDef.symbol.dependencyAnnotations.map((processDependency _).tupled)
            val definitions = List(js.RawCodeBlock(code))
            js.Program(dependencies ++ definitions)
        }.getOrElse {
            js.Program.empty
        }

        (classDef.symbol.definitionIdentifier, outputProgram)
    }

    private implicit class RichSymbol(s: Symbol)
    {
        def definitionType = {
            require(s.isClass)

            if (s.isPackageObjectClass) PackageObjectDefinition else
            if (s.isModuleClass) ObjectDefinition else
            if (s.isTrait) TraitDefinition else ClassDefinition
        }

        def definitionIdentifier = {
            val fullIdentifier = definitionType match {
                case PackageObjectDefinition => s.owner.fullName + "$"
                case dt => {
                    val packagePrefix = s.enclosingPackage.fullName + "."
                    val objectSuffix = if (dt == ObjectDefinition) "$" else ""
                    packagePrefix + s.fullName.stripPrefix(packagePrefix).replace('.', '#') + objectSuffix
                }
            }

            fullIdentifier.stripPrefix("<empty>.").stripPrefix("__root__.")
        }

        def isCompiled = !(isIgnored || isAdapter)

        def isIgnored = hasAnnotation(typeOf[api.ignored])

        def isAdapter = hasAnnotation(typeOf[api.adapter])

        def nativeAnnotation: Option[String] = typedAnnotation(typeOf[api.native]).map { i =>
            i.stringArg(0).getOrElse {
                throw new CompilationException("The jsCode argument of the @native annotation must be a constant.")
            }
        }

        def dependencyAnnotations = typedAnnotations(typeOf[api.dependency]).map { i =>
            val dependencyType = i.constantAtIndex(0).map(_.typeValue).getOrElse {
                throw new CompilationException("The cls argument of the @dependency annotation is invalid.")
            }
            val isHard = i.constantAtIndex(1).map(_.booleanValue).getOrElse {
                throw new CompilationException("The isHard argument of the @dependency annotation must be a constant.")
            }
            (dependencyType, isHard)
        }

        def hasAnnotation(tpe: Type) = typedAnnotation(tpe).nonEmpty

        def typedAnnotation(tpe: Type) = typedAnnotations(tpe).headOption

        def typedAnnotations(tpe: Type) = s.annotations.filter(_.atp == tpe)
    }
}
