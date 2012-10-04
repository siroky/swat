package swat.compiler.frontend

import swat.api
import swat.compiler.{SwatCompilerPlugin, CompilationException}

trait RichTrees
{
    self: SwatCompilerPlugin =>

    import global._

    implicit class RichSymbol(s: Symbol)
    {
        def definitionType = {
            require(s.isClass)

            if (s.isPackageObjectClass) PackageObjectDefinition else
            if (s.isModuleClass) ObjectDefinition else
            if (s.isTrait) TraitDefinition else ClassDefinition
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
            dependencyType -> isHard
        }

        def hasAnnotation(tpe: Type) = typedAnnotation(tpe).nonEmpty

        def typedAnnotation(tpe: Type) = typedAnnotations(tpe).headOption

        def typedAnnotations(tpe: Type) = s.annotations.filter(_.atp == tpe)
    }
}
