package swat.compiler.frontend

import tools.nsc.Global
import swat.api
import swat.compiler.{CompilationException, js}

trait JsAstGenerator
{
    self: {val global: Global} =>

    import global._

    def processCompilationUnit(compilationUnit: CompilationUnit): Map[ArtifactRef, js.Program] = {
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

    private def processClassDef(classDef: ClassDef): (ArtifactRef, js.Program) = {
        val artifactType = classDef.symbol.artifactType
        val fullName = artifactType match {
            case PackageObjectArtifact => classDef.symbol.owner.fullName
            case _ => classDef.symbol.fullName
        }

        val outputProgram = classDef.symbol.nativeAnnotation.map { a =>
            js.Program(List(js.RawCodeBlock(a.jsCode)))
        }.getOrElse {
            js.Program.empty
        }

        (ArtifactRef(artifactType, fullName), outputProgram)
    }

    private implicit class RichSymbol(s: Symbol)
    {
        def artifactType: ArtifactType = {
            if (s.isPackageObjectClass) PackageObjectArtifact else
            if (s.isModuleClass) ObjectArtifact else
            if (s.isTrait) TraitArtifact else
            if (s.isClass) ClassArtifact else {
                throw new UnsupportedOperationException("The type doesn't correspond to an artifact.")
            }
        }

        def isCompiled = !(isIgnored || isAdapter)

        def isIgnored = hasAnnotation(typeOf[api.ignored])

        def isAdapter = hasAnnotation(typeOf[api.adapter])

        def nativeAnnotation = typedAnnotation(typeOf[api.native]).map { i =>
            val code = i.stringArg(0).getOrElse {
                throw new CompilationException("The jsCode in the @native annotation has to be a string literal.")
            }
            new api.native(code)
        }

        def dependencyAnnotations = typedAnnotations(typeOf[api.dependency])

        def hasAnnotation(tpe: Type) = typedAnnotation(tpe).nonEmpty

        def typedAnnotation(tpe: Type) = typedAnnotations(tpe).headOption

        def typedAnnotations(tpe: Type) = s.annotations.filter(_.atp == tpe)
    }
}
