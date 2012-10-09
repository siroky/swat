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

        def processReturnTree(tree: Tree): js.Statement = {
            val processedTree = processExpressionTree(tree)

            // If the type of the tree is Unit, then the tree appears on the return position of an expression, which
            // actually doesn't return anything. So the 'return' may be omitted.
            if (tree.tpe =:= typeOf[Unit]) {
                tree match {
                    // If the tree is a Block with structure { statement; (); } then the scope of the block created in
                    // the processBlock method may be omitted. The scope protects from shadowing and using the shadowed
                    // value instead of the original value. However it's not possible to shadow and use a variable in
                    // one statement, which isn't itself scoped. The purpose is to get rid of unnecessary scoping.
                    case Block(statement :: Nil, Literal(Constant(_: Unit))) => processStatementTree(statement)
                    case _ => js.ExpressionStatement(processedTree)
                }
            } else {
                js.ReturnStatement(Some(processedTree))
            }
        }

        def processStatementTrees(trees: Seq[Tree]): Seq[js.Statement] = trees.map(processStatementTree _)

        def processExpressionTrees(trees: Seq[Tree]): Seq[js.Expression] = trees.map(processExpressionTree _)

        def processBlock(block: Block): js.Expression = block.toMatchBlock match {
            case Some(m: MatchBlock) => processMatchBlock(m)
            case _ => scoped {
                processStatementTrees(block.stats) :+ processReturnTree(block.expr)
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
                swatMethodCall("classOf", js.RawCodeExpression(definitionIdentifier(t.typeSymbol))) // TODO names
            }
            case l => {
                error("Unexpected type of a literal (%s)".format(l))
                js.UndefinedLiteral
            }
        }

        def processIdent(identifier: Ident): js.Identifier = {
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
            case s @ Select(q, _) if q.tpe <:< typeOf[AnyVal] => processAnyValMethod(s.symbol, q, apply.args)
            case Select(n: New, selectName) if selectName.toString == "<init>" => processNew(n, apply.args)
            case f => js.CallExpression(processExpressionTree(f), processExpressionTrees(apply.args))
        }

        def processAnyValMethod(method: Symbol, qualifier: Tree, args: Seq[Tree]): js.Expression = {
            if (method.isAnyValOperator || method.isEqualsMethod) {
                processAnyValOperator(method, qualifier, args.headOption)
            } else {
                // Primitive values in JavaScript aren't objects, so methods can't be invoked on them. It's possible
                // to convert a primitive value to an object wrapper (e.g. Number), however these wrappers would have
                // to be extended so they'd provide all methods of Scala primitive values. Consequently, integration
                // of existing libraries would be problematic, if the Scala version of the method overriden something
                // that was defined there by the library. The philosophy of Swat is not to interfere with the
                // environment in any way. So the primitive value methods are defined on the companion objects instead.
                // For example Scala code '123.toDouble' produces JavaScript code 'scala.Int.toDouble(3)'.
                //
                // Pros and cons of native object extensions can be found here:
                // http://perfectionkills.com/extending-built-in-native-objects-evil-or-not/

                val processedArgs = processExpressionTrees(List(qualifier) ++ args) // TODO names
                objectMethodCall(qualifier.tpe.typeSymbol.companionModule, method.name.toString, processedArgs: _*)
            }
        }

        def processAnyValOperator(operatorSymbol: Symbol, operand1: Tree, operand2: Option[Tree]): js.Expression = {
            // Convert the Scala operator name to JavaScript operator. Luckily, all are the same as in Scala.
            val operatorReplacements = Map(
                "unary_" -> "", "$plus" -> "+", "$minus" -> "-", "$times" -> "*", "$div" -> "/", "$percent" -> "%",
                "$amp" -> "&", "$bar" -> "|", "$up" -> "^", "$less" -> "<", "$greater" -> ">", "$tilde" -> "~",
                "$eq" -> "=", "$bang" -> "!", "equals" -> "=="
            )
            val operator = operatorReplacements.foldLeft(operatorSymbol.nameString)((o, r) => o.replace(r._1, r._2))

            // Chars, that are represented as strings, need to be explicitly converted to integers, so arithmetic
            // operations would work on them. The first operand should be converted iff the class owning the operator
            // method is the scala.Char. The second operand should be converted iff the the operator method takes
            // scala.Char as the first argument.
            def processOperand(operand: Tree, isFirst: Boolean = true): js.Expression = {
                val processedOperand = processExpressionTree(operand)
                def shouldConvertFirst = isFirst && operatorSymbol.owner.tpe =:= typeOf[Char]
                def shouldConvertSecond = !isFirst && operatorSymbol.tpe.paramTypes.head =:= typeOf[Char]
                if (!operatorSymbol.isEqualsMethod && (shouldConvertFirst || shouldConvertSecond)) {
                    objectMethodCall(typeOf[Char].typeSymbol.companionModule, "toInt", processedOperand)
                } else {
                    processedOperand
                }
            }

            val expr = operand2.map { o2 =>
                js.InfixExpression(processOperand(operand1), operator, processOperand(o2, isFirst = false))
            }.getOrElse {
                js.PrefixExpression(operator, processOperand(operand1))
            }

            operator match {
                case "/" if operand1.tpe.typeSymbol.isIntegralValueClass => {
                    // All numbers are represented as doubles, so even if they're integral, their division can yield a
                    // double. E.g. 3 / 2 == 1.5. To ensure the same behavior as in Scala, division results have to be
                    // floored in case that the first operand is of integral type.
                    methodCall(js.Identifier("Math"), "floor", expr)
                }
                case "&" | "|" | "^" if operatorSymbol.isBooleanValOperator => {
                    // The long-circuited logical operations aren't directly supported in JavaScript. But if they're
                    // used on booleans, then the operands are converted to numbers. A result of the corresponding
                    // bitwise is therefore also a number, which has to be converted back to a boolean.
                    js.CallExpression(js.Identifier("Boolean"), List(expr))
                }
                case _ => expr
            }
        }

        def processTyped(typed: Typed): js.Expression = {
            if (typed.expr.tpe <:< typed.tpt.tpe) {
                // No type cast is necessary since it's already proven that the expr is of the specified type.
                processExpressionTree(typed.expr)
            } else {
                // TODO
                swatMethodCall("asInstanceOf", processExpressionTree(typed.expr))
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
                unScoped(processReturnTree(condition.thenp)),
                unScoped(processReturnTree(condition.elsep)))
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

        def processLoop(loop: Loop): js.Expression = {
            // Because the whole loop is scoped, the stats may be unscoped (double scoping isn't necessary). As a
            // consequence, all top level return statements have to be omitted. Otherwise it'd terminate the loop.
            val processedStats = processStatementTrees(loop.stats).flatMap(unScoped _).map {
                case js.ReturnStatement(Some(e)) => js.ExpressionStatement(e)
                case s => s
            }

            scoped {
                js.WhileStatement(processExpressionTree(loop.expr), processedStats, loop.isDoWhile)
            }
        }

        def processMatchBlock(matchBlock: MatchBlock): js.Expression = {
            val processedInit = processStatementTrees(matchBlock.init)
            val processedCases = matchBlock.cases.map { c =>
                val name = Some(js.Identifier(c.name.toString)) // TODO names
                val params = c.params.map(processIdent _)
                val body = processStatementTree(c.rhs)
                js.ExpressionStatement(js.FunctionExpression(name, params, List(body)))
            }
            val matchResult = js.ReturnStatement(Some(js.CallExpression(js.Identifier(matchBlock.cases.head.name), Nil))) // TODO names

            scoped {
                processedInit ++ processedCases ++ List(matchResult)
            }
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
