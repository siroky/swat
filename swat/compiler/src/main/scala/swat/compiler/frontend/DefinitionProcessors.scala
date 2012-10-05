package swat.compiler.frontend

import swat.compiler.SwatCompilerPlugin
import swat.compiler.js
import collection.mutable

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
            val body = processStatementTree(defDef.rhs) match {
                case js.ExpressionStatement(js.CallExpression(js.FunctionExpression(_, _, b), _)) => js.Block(b)
                case b => b
            }
            List(js.AssignmentStatement(
                // TODO names
                memberChain(js.RawCodeExpression(definitionIdentifier(definition.symbol)), "prototype", defDef.name.toString),
                js.FunctionExpression(None, Nil, List(body))
            ))
        }

        def processTree(tree: Tree): js.Ast = tree match {
            case EmptyTree => js.UndefinedLiteral
            case b: Block => processBlock(b)
            case l: Literal => processLiteral(l)
            case i: Ident => processIdent(i)
            case t: Typed => processTyped(t)
            case s: Select => processSelect(s)
            case a: Apply => processApply(a)
            case v: ValDef => processValDef(v)
            case i: If => processIf(i)
            case l: LabelDef => processLabelDef(l)
            case t: Throw => processThrow(t)
            case _ => {
                error("Not implemented Scala language feature %s: %s".format(tree.getClass, tree.toString()))
                js.UndefinedLiteral
            }
        }

        def processStatementTree(tree: Tree): js.Statement = processTree(tree) match {
            case s: js.Statement => s
            case e: js.Expression => js.ExpressionStatement(e)
            case _ => {
                error("A non-statement tree found on a statement position (%s)".format(tree))
                js.Block(Nil)
            }
        }

        def processExpressionTree(tree: Tree): js.Expression = processTree(tree) match {
            case e: js.Expression => e
            case _ => {
                error("A non-expression tree found on an expression position (%s)".format(tree))
                js.UndefinedLiteral
            }
        }

        def processStatementTrees(trees: Seq[Tree]): Seq[js.Statement] = trees.map(processStatementTree _)

        def processExpressionTrees(trees: Seq[Tree]): Seq[js.Expression] = trees.map(processExpressionTree _)

        def processBlock(block: Block): js.Expression = {
            def traverseBlock(block: Block): (Seq[Tree], Tree) = block.expr match {
                case nestedBlock: Block => {
                    val (nestedStats, nestedExpr) = traverseBlock(nestedBlock)
                    (block.stats ++ nestedStats, nestedExpr)
                }
                case e => (block.stats, e)
            }

            val (stats, expr) = traverseBlock(block)
            val processedExpr = processExpressionTree(expr)
            val exprStatement =
                if (expr.tpe == typeOf[Unit]) {
                    js.ExpressionStatement(processedExpr)
                } else {
                    js.ReturnStatement(Some(processedExpr))
                }

            immediateAnonymousInvocation {
                processStatementTrees(stats) ++ List(exprStatement)
            }
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
            case ErrorType => js.UndefinedLiteral
            case t: Type => {
                dependencies += t.typeSymbol -> false
                swatMethodInvocation("classOf", js.RawCodeExpression(definitionIdentifier(t.typeSymbol)))
            }
            case l => {
                error("Unexpected type of a literal (%s)".format(l))
                js.UndefinedLiteral
            }
        }

        def processIdent(identifier: Ident): js.Expression = {
            js.Identifier(identifier.name.toString) // TODO names
        }

        def processSelect(select: Select): js.Expression = {
            // TODO
            // TODO names
            val selectName = select.name.toString
            val processedQualifier = processExpressionTree(select.qualifier)

            if (selectName == "<init>") {
                processedQualifier
            } else {
                js.MemberExpression(processedQualifier, js.Identifier(selectName))
            }
        }

        def processApply(apply: Apply): js.Expression = apply.fun match {
            // TODO
            case Select(n: New, selectName) if selectName.toString == "<init>" => processNew(n, apply.args)
            case f => js.CallExpression(processExpressionTree(f), processExpressionTrees(apply.args))
        }

        def processTyped(typed: Typed): js.Expression = {
            if (typed.expr.tpe <:< typed.tpt.tpe) {
                // No type cast is necessary since it's already proven that the expr is of the specified type.
                processExpressionTree(typed.expr)
            } else {
                // TODO
                swatMethodInvocation("asInstanceOf", processExpressionTree(typed.expr))
            }
        }

        def processValDef(valDef: ValDef): js.Statement = {
            // TODO names
            js.VariableStatement(List(js.Identifier(valDef.name.toString) -> Some(processExpressionTree(valDef.rhs))))
        }

        def processNew(n: New, args: Seq[Tree]): js.Expression = {
            val definitionExpr = js.RawCodeExpression(definitionIdentifier(n.tpe.typeSymbol)) // TODO names
            js.NewExpression(js.CallExpression(definitionExpr, processExpressionTrees(args)))
        }

        def processIf(condition: If): js.Expression = immediateAnonymousInvocation {
            js.IfStatement(
                processExpressionTree(condition.cond),
                List(js.ReturnStatement(Some(processExpressionTree(condition.thenp)))),
                List(js.ReturnStatement(Some(processExpressionTree(condition.elsep)))))
        }

        def processLabelDef(labelDef: LabelDef): js.Expression = {
            if (labelDef.name.startsWith("while$")) {
                processWhile(labelDef, isDoWhile = false)
            } else if (labelDef.name.startsWith("doWhile$")) {
                processWhile(labelDef, isDoWhile = true)
            } else {
                error("Unexpected type of a label (%s)".format(labelDef))
                js.UndefinedLiteral
            }
        }

        def processWhile(labelDef: LabelDef, isDoWhile: Boolean): js.Expression = {
            val labelName = labelDef.name.toString
            val (expr, stats) = labelDef.rhs match {
                case Block(s, Apply(Ident(n), _)) if n.toString == labelName => (Literal(Constant(true)), s)
                case If(e, Block(s, Apply(Ident(n), _)), _) if n.toString == labelName => (e, s)
                case Block(s, If(e, Apply(Ident(n), _), _)) if n.toString == labelName => (e, s)
                case _ => {
                    error("Unknown format of a while loop label (%s)".format(labelDef))
                    (EmptyTree, Nil)
                }
            }

            immediateAnonymousInvocation {
                js.WhileStatement(processExpressionTree(expr), processStatementTrees(stats), isDoWhile)
            }
        }

        def processThrow(t: Throw): js.Expression = immediateAnonymousInvocation {
            js.ThrowStatement(processExpressionTree(t.expr))
        }
    }

    private class ClassProcessor extends DefinitionProcessor
    private class TraitProcessor extends DefinitionProcessor
    private class ObjectProcessor extends DefinitionProcessor
    private class PackageObjectProcessor extends DefinitionProcessor
}
