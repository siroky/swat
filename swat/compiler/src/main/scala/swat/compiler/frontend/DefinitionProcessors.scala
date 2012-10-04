package swat.compiler.frontend

import swat.compiler.SwatCompilerPlugin
import swat.compiler.js
import collection.mutable
import js.Expression

trait DefinitionProcessors
{
    self: SwatCompilerPlugin with ScalaAstProcessor =>

    import global._

    object DefinitionProcessor
    {
        def apply(definitionType: DefinitionType): DefinitionProcessor = definitionType match {
            case ClassDefinition => new ClassProcessor
            case TraitDefinition => new TraitProcessor
            case ObjectDefinition => new ObjectProcessor
            case PackageObjectDefinition => new PackageObjectProcessor
        }
    }

    class DefinitionProcessor
    {
        private val dependencies = mutable.ListBuffer.empty[(Symbol, Boolean)]

        def process(definition: ClassDef): Seq[js.Statement] = {
            dependencies.clear()
            definition.impl.body.flatMap {
                case d: DefDef if !d.symbol.isConstructor => processDefDef(definition, d)
                case _ => Nil
            }
        }

        def processDefDef(definition: ClassDef, defDef: DefDef): Seq[js.Statement] = {
            // TODO just temporary solution to enable testing of code fragments.
            val body = processTreeToStatement(defDef.rhs).toList
            List(js.AssignmentStatement(
                memberChain(js.RawCodeExpression(definitionIdentifier(definition.symbol)), "prototype", defDef.name.toString),
                js.FunctionExpression(None, Nil, body)
            ))
        }

        def processTree(tree: Tree): js.Ast = tree match {
            case b: Block => processBlock(b)
            case l: Literal => processLiteral(l)
            case _ => {
                error("Not implemented Scala language feature %s: %s".format(tree.getClass, tree.toString()))
                js.UndefinedLiteral
            }
        }

        def processTreeToStatement(tree: Tree): Option[js.Statement] = processTree(tree) match {
            case e: js.Expression => Some(js.ExpressionStatement(e))
            case s: js.Statement => Some(s)
            case _ => None
        }

        def processTreeToExpression(tree: Tree): Option[Expression] = processTree(tree) match {
            case e: js.Expression => Some(e)
            case js.ExpressionStatement(e) => Some(e)
            case _ => None
        }

        def processBlock(block: Block): js.Block = {
            val processedStats = block.stats.flatMap(processTreeToStatement _)
            val processedExpr = block.expr match {
                case b: Block => Some(processBlock(b))
                case e => processTreeToExpression(e).map(e => js.ReturnStatement(Some(e)))
            }
            js.Block(processedStats ++ processedExpr.toList)
        }

        def processLiteral(literal: Literal): js.Expression = literal.value.value match {
            case () => js.UndefinedLiteral
            case null => js.NullLiteral
            case b: Boolean => js.BooleanLiteral(b)
            case c: Char => js.StringLiteral(c.toString)
            case s: String => js.StringLiteral(s)
            case b: Byte => js.NumericLiteral(b)
            case s: Short => js.NumericLiteral(s)
            case i: Int => js.NumericLiteral(i)
            case l: Long => js.NumericLiteral(l)
            case f: Float => js.NumericLiteral(f)
            case d: Double => js.NumericLiteral(d)
            case t: TypeRef => {
                dependencies += t.typeSymbol -> false
                swatMethodInvocation("classOf", js.RawCodeExpression(definitionIdentifier(t.typeSymbol)))
            }
        }
    }

    private class ClassProcessor extends DefinitionProcessor
    private class TraitProcessor extends DefinitionProcessor
    private class ObjectProcessor extends DefinitionProcessor
    private class PackageObjectProcessor extends DefinitionProcessor
}
