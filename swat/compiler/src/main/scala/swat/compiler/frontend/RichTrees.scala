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

    implicit class RichBlock(b: Block)
    {
        def toMatchBlock: Option[MatchBlock] = {
            val (init, labels) = (b.stats ++ List(b.expr)).span(!_.isInstanceOf[LabelDef])
            val cases = labels.collect {
                case l @ LabelDef(n, _, _) if n.toString.matches("^(case|matchEnd)[0-9]*$") => l
            }

            if (labels.nonEmpty && cases.length == labels.length) {
                Some(MatchBlock(init, cases))
            } else {
                None
            }
        }
    }

    case class MatchBlock(init: Seq[Tree], cases: Seq[LabelDef])

    implicit class RichLabelDef(l: LabelDef)
    {
        def isLoop = toLoop.isEmpty

        def toLoop: Option[Loop] = {
            val labelName = l.name.toString
            val isWhile = labelName.startsWith("while$")
            val isDoWhile = labelName.startsWith("doWhile$")

            if (isWhile || isDoWhile) {
                val (expr, stats) = l.rhs match {
                    case Block(s, Apply(Ident(n), _)) if n.toString == labelName => (Literal(Constant(true)), s)
                    case If(e, Block(s, Apply(Ident(n), _)), _) if n.toString == labelName => (e, s)
                    case Block(s, If(e, Apply(Ident(n), _), _)) if n.toString == labelName => (e, s)
                    case _ => {
                        error("Unknown format of a while loop label (%s)".format(l))
                        (EmptyTree, Nil)
                    }
                }
                Some(Loop(expr, stats, isDoWhile))

            } else {
                None
            }
        }
    }

    case class Loop(expr: Tree, stats: Seq[Tree], isDoWhile: Boolean)
}
