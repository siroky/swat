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
                error("Unknown Scala construct %s: %s".format(tree.getClass, tree.toString()))
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

        def processBlock(block: Block): js.Expression = block.toMatchBlock match {
            case Some(m: MatchBlock) => processMatchBlock(m)
            case _ => {
                val processedExpr = processExpressionTree(block.expr)
                val processedStats = processStatementTrees(block.stats)

                // If the block doesn't return anything and the expr is a Block consisting of a tree and Unit object,
                // then it may be unscoped. The scope protects from shadowing and using the shadowed value instead of
                // the original value. However it's not possible to shadow and use a variable in one statement, which
                // isn't itself scoped. The purpose is to get rid of unnecessary scoping.
                val unscopedExpr =
                    if (block.expr.tpe == typeOf[Unit]) {
                        block.expr match {
                            case Block(s, Literal(Constant(_: Unit))) if (s.length == 1) => unscoped(processedExpr)
                            case _ => List(js.ExpressionStatement(processedExpr))
                        }
                    } else {
                        List(js.ReturnStatement(Some(processedExpr)))
                    }

                scoped {
                    processedStats ++ unscopedExpr
                }
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

        def processIf(condition: If): js.Expression = scoped {
            js.IfStatement(
                processExpressionTree(condition.cond),
                List(js.ReturnStatement(Some(processExpressionTree(condition.thenp)))),
                List(js.ReturnStatement(Some(processExpressionTree(condition.elsep)))))
        }

        def processLabelDef(labelDef: LabelDef): js.Expression = {
            labelDef.toLoop match {
                case Some(l: Loop) => processLoop(l)
                case _ => {
                    error("Unexpected type of a label (%s)".format(labelDef))
                    js.UndefinedLiteral
                }
            }
        }

        def processLoop(loop: Loop): js.Expression = scoped {
            js.WhileStatement(processExpressionTree(loop.expr), processStatementTrees(loop.stats), loop.isDoWhile)
        }

        def processMatchBlock(matchBlock: MatchBlock): js.Expression = scoped {
            js.EmptyStatement
        }

        def processThrow(t: Throw): js.Expression = scoped {
            js.ThrowStatement(processExpressionTree(t.expr))
        }
    }

    private class ClassProcessor extends DefinitionProcessor
    private class TraitProcessor extends DefinitionProcessor
    private class ObjectProcessor extends DefinitionProcessor
    private class PackageObjectProcessor extends DefinitionProcessor
}
