package swat.compiler.frontend

import swat.compiler.SwatCompilerPlugin
import swat.compiler.js
import collection.mutable

trait ClassDefProcessors
{
    self: SwatCompilerPlugin with ScalaAstProcessor =>

    import global._

    object ClassDefProcessor
    {
        def apply(classDef: ClassDef): ClassDefProcessor = classDef.symbol.classSymbolKind match {
            case ClassSymbol => new ClassProcessor(classDef)
            case TraitSymbol => new TraitProcessor(classDef)
            case ObjectSymbol => new ObjectProcessor(classDef)
            case PackageObjectSymbol => new PackageObjectProcessor(classDef)
        }
    }

    class ClassDefProcessor(private val classDef: ClassDef)
    {
        private val dependencies = mutable.ListBuffer.empty[(Type, Boolean)]

        private val equalityOperatorMap = Map("equals" -> "==", "eq" -> "===", "ne" -> "!==")

        private val self = localJsIdentifier("self")

        private val outer = localJsIdentifier("$outer")

        def process(): Seq[js.Statement] = {
            classDef.impl.body.flatMap {
                case d: DefDef if !d.symbol.isConstructor => processDefDef(classDef, d)
                case _ => Nil
            }
        }

        def processDefDef(classDef: ClassDef, defDef: DefDef): Seq[js.Statement] = {
            // TODO just temporary solution to enable testing of code fragments.
            List(js.AssignmentStatement(
                memberChain(typeJsIdentifier(classDef.symbol.tpe), "prototype", defDef.name.toString),
                js.FunctionExpression(None, Nil, unScoped(processReturnTree(defDef.rhs)))
            ))
        }

        def processTree(tree: Tree): js.Ast = tree match {
            case EmptyTree => js.UndefinedLiteral
            case b: Block => processBlock(b)
            case l: Literal => processLiteral(l)
            case t: TypeTree => processTypeTree(t)
            case a: ArrayValue => processArrayValue(a)
            case i: Ident => processIdent(i)
            case t: This => processThis(t)
            case s: Select => processSelect(s)
            case a: Apply => processApply(a)
            case t: TypeApply => processTypeApply(t)
            case t: Typed => processTyped(t)
            case a: Assign => processAssign(a)
            case v: ValDef => processValDef(v)
            case d: DefDef => processLocalDefDef(d)
            case i: If => processIf(i)
            case l: LabelDef => processLabelDef(l)
            case t: Throw => processThrow(t)
            case t: Try => processTry(t)
            case _ => {
                error("Unknown Scala construct %s: %s".format(tree.getClass, tree.toString))
                js.UndefinedLiteral
            }
        }

        def processStatementTree(tree: Tree): js.Statement = processTree(tree) match {
            case s: js.Statement => s
            case e: js.Expression => js.ExpressionStatement(e)
            case _ => {
                error(s"A non-statement tree found on a statement position ($tree)")
                js.Block(Nil)
            }
        }

        def processExpressionTree(tree: Tree): js.Expression = processTree(tree) match {
            case e: js.Expression => e
            case _ => {
                error(s"A non-expression tree found on an expression position ($tree)")
                js.UndefinedLiteral
            }
        }

        def processReturnTree(tree: Tree): js.Statement = {
            val processedTree = processExpressionTree(tree)

            // If the type of the tree is Unit, then the tree appears on the return position of an expression, which
            // actually doesn't return anything. So the 'return' may be omitted.
            if (tree.tpe.isUnit) {
                tree match {
                    // If the tree is a Block with structure { statement; (); } then the block that wraps the statement
                    // may be omitted. The scope protects from shadowing and using the shadowed value instead of the
                    // original value. However it's not possible to shadow and use a variable in one statement, which
                    // isn't itself scoped. The purpose is to get rid of unnecessary scoping.
                    case Block(statement :: Nil, Literal(Constant(_: Unit))) => processStatementTree(statement)
                    case _ => js.ExpressionStatement(processedTree)
                }
            } else {
                js.ReturnStatement(Some(processedTree))
            }
        }

        def processStatementTrees(trees: Seq[Tree]): Seq[js.Statement] = trees.map(processStatementTree _)

        def processExpressionTrees(trees: Seq[Tree]): Seq[js.Expression] = trees.map(processExpressionTree _)

        def processBlock(block: Block): js.Expression = block match {
            case Block(List(c: ClassDef), _) if c.symbol.isAnonymousFunction => processAnonymousFunction(c)
            case b => b.toMatchBlock match {
                case Some(m: MatchBlock) => processMatchBlock(m)
                case _ => scoped {
                    val processedExpr = processReturnTree(b.expr)

                    // If the block contains just the expr, then the expr doesn't have to be scoped, because there
                    // isn't anything to protect from shadowing in the block (the stats are empty)
                    val unScopedExpr = if (b.stats.isEmpty) unScoped(processedExpr) else List(processedExpr)
                    processStatementTrees(b.stats) ++ unScopedExpr
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
                dependencies += t -> false
                swatMethodCall("classOf", typeJsIdentifier(t))
            }
            case l => {
                error(s"Unexpected type of a literal ($l)")
                js.UndefinedLiteral
            }
        }

        def processTypeTree(typeTree: TypeTree): js.Expression = {
            val tpe = typeTree.tpe.underlying
            dependencies += tpe -> false
            typeJsIdentifier(tpe)
        }

        def processArrayValue(arrayValue: ArrayValue): js.Expression =
            objectMethodCall(typeOf[Array[_]].companionSymbol, "apply", arrayValue.elems.map(processExpressionTree _))

        def processAnonymousFunction(functionClassDef: ClassDef): js.Expression = {
            val applyDefDef = functionClassDef.impl.body.collect { case d: DefDef if d.symbol.isApplyMethod => d }.head
            val parameters = applyDefDef.vparamss.flatten.map(v => localJsIdentifier(v.name))
            js.FunctionExpression(None, parameters, unScoped(processReturnTree(applyDefDef.rhs)))
        }

        def processIdent(identifier: Ident): js.Identifier = localJsIdentifier(identifier.name)

        def processThis(t: This): js.Expression = {
            if (t.symbol.isPackageClass) {
                packageJsIdentifier(t.symbol)
            } else {
                // The t.symbol isn't a package, therefore it's the current class or an outer class.
                def getNestingDepth(innerClass: Symbol): Int = {
                    if (innerClass == t.symbol) 0 else getNestingDepth(innerClass.owner) + 1
                }
                val depth = getNestingDepth(classDef.symbol)
                (1 to depth).foldLeft[js.Expression](self)((z, _) => js.MemberExpression(z, outer))
            }
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
            // Methods on types that compile to JavaScript primitive types.
            case s @ Select(q, _) if q.tpe.isAnyValOrString => processAnyValOrStringMethodCall(s.symbol, q, apply.args)

            // Standard methods of the Any class.
            case s @ Select(q, _) if s.symbol.isAnyMethodOrOperator => processAnyMethodCall(s.symbol, q, apply.args)
            case TypeApply(s @ Select(q, _), typeArgs) if s.symbol.isAnyMethodOrOperator => {
                processAnyMethodCall(s.symbol, q, apply.args ++ typeArgs)
            }
            case s @ Select(q, _) if q.tpe.isFunction => processFunctionMethodCall(s.symbol, q, apply.args)

            // TODO
            case Select(n: New, selectName) if selectName.toString == "<init>" => processNew(n, apply.args)
            case f => js.CallExpression(processExpressionTree(f), processExpressionTrees(apply.args))
        }

        def dispatchCallToCompanion(method: Symbol, qualifier: Tree, args: Seq[Tree]): js.Expression = {
            objectMethodCall(qualifier.tpe.companionSymbol, method, processExpressionTrees(qualifier +: args))
        }

        def processAnyValOrStringMethodCall(method: Symbol, qualifier: Tree, args: Seq[Tree]): js.Expression = {
            if (method.isAnyValOrStringOperator || method.isEqualityOperator) {
                processAnyValOrStringOperator(method, qualifier, args.headOption)
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
                // http://perfectionkills.com/extending-built-in-native-objects-evil-or-no
                if (method.isAnyMethodOrOperator) {
                    processAnyMethodCall(method, qualifier, args)
                } else {
                    dispatchCallToCompanion(method, qualifier, args)
                }
            }
        }

        def processAnyValOrStringOperator(symbol: Symbol, operand1: Tree, operand2: Option[Tree]): js.Expression = {
            // Convert the Scala operator name to JavaScript operator. Luckily, all are the same as in Scala.
            val operator = equalityOperatorMap.foldLeft(symbol.nameString.stripPrefix("unary_")) { (o, r) =>
                o.replace(r._1, r._2)
            }

            // Chars, that are represented as strings, need to be explicitly converted to integers, so arithmetic
            // operations would work on them.
            def processOperand(operand: Tree): js.Expression = {
                val processedOperand = processExpressionTree(operand)
                if (!symbol.isEqualityOperator && operand.tpe.isChar) {
                    objectMethodCall(typeOf[Char].companionSymbol, localIdentifier("toInt"), List(processedOperand))
                } else {
                    processedOperand
                }
            }

            val expr = operand2.map { o2 =>
                js.InfixExpression(processOperand(operand1), operator, processOperand(o2))
            }.getOrElse {
                js.PrefixExpression(operator, processOperand(operand1))
            }

            operator match {
                case "/" if operand1.tpe.isIntegralVal => {
                    // All numbers are represented as doubles, so even if they're integral, their division can yield a
                    // double. E.g. 3 / 2 == 1.5. To ensure the same behavior as in Scala, division results have to be
                    // floored in case that the first operand is of integral type.
                    methodCall(js.Identifier("Math"), "floor", expr)
                }
                case "&" | "|" | "^" if symbol.isBooleanValOperator => {
                    // The long-circuited logical operations aren't directly supported in JavaScript. But if they're
                    // used on booleans, then the operands are converted to numbers. A result of the corresponding
                    // bitwise is therefore also a number, which has to be converted back to a boolean.
                    js.CallExpression(js.Identifier("Boolean"), List(expr))
                }
                case _ => expr
            }
        }

        def processAnyMethodCall(method: Symbol, qualifier: Tree, args: Seq[Tree]): js.Expression = {
            val processedQualifier = processExpressionTree(qualifier)
            if (method.isEqualityOperator) {
                val processedOperand2 = processExpressionTree(args.head)
                val equalityExpr = swatMethodCall(localIdentifier("equals"), processedQualifier, processedOperand2)

                method.nameString match {
                    case "==" | "equals" => equalityExpr
                    case "!=" => js.PrefixExpression("!", equalityExpr)
                    case o => js.InfixExpression(processedQualifier, equalityOperatorMap(o), processedOperand2)
                }
            } else {
                val methodName = method.nameString.replace("##", "hashCode")
                swatMethodCall(localIdentifier(methodName), processExpressionTrees(qualifier +: args): _*)
            }
        }

        def processFunctionMethodCall(method: Symbol, qualifier: Tree, args: Seq[Tree]): js.Expression = {
            val processedQualifier = processExpressionTree(qualifier)
            val processedArgs = args.map(processExpressionTree _)
            if (method.isApplyMethod) {
                js.CallExpression(processedQualifier, processedArgs)
            } else {
                dispatchCallToCompanion(method, qualifier, args)
            }
        }

        def processTypeApply(typeApply: TypeApply): js.Expression = {
            // The methods where the type actually matters (e.g. isInstanceOf) are processed earlier. In other cases
            // the type application may be omitted.
            processExpressionTree(typeApply.fun)
        }

        def processTyped(typed: Typed): js.Expression = {
            if (typed.expr.tpe.underlying <:< typed.tpt.tpe.underlying) {
                // No type cast is necessary since it's already proven that the expr is of the specified type.
                processExpressionTree(typed.expr)
            } else {
                error(s"Unexpected typed expression ($typed)")
                js.UndefinedLiteral
            }
        }

        def processAssign(assign: Assign): js.Statement =
            js.AssignmentStatement(processExpressionTree(assign.lhs), processExpressionTree(assign.rhs))

        def processValDef(valDef: ValDef): js.Statement = {
            if (valDef.symbol.isLazy && valDef.name.endsWith("$lzy")) {
                // The val definition associated with the lazy val can be omitted as the value will be stored in the
                // corresponding function (see processLocalDefDef method).
                js.EmptyStatement
            } else {
                js.VariableStatement(localJsIdentifier(valDef.name), Some(processExpressionTree(valDef.rhs)))
            }
        }

        def processLazyVal(defDef: DefDef): js.Statement = defDef.rhs match {
            case Block(List(Assign(_, rhs)), _) => {
                val initializer = js.FunctionExpression(None, Nil, unScoped(processReturnTree(rhs)))
                val value = swatMethodCall("memoize", initializer)
                js.VariableStatement(localJsIdentifier(defDef.name), Some(value))
            }
            case _ => {
                error("Unexpected lazy val initializer (%s)".format(defDef.rhs))
                js.EmptyStatement
            }
        }

        def processLocalDefDef(defDef: DefDef): js.Statement = {
            if (defDef.symbol.isLazy) {
                processLazyVal(defDef)
            } else {
                // Check whether the function is nested in a local function with the same name which isn't supported.
                def checkNameDuplicity(symbol: Symbol) {
                    if (symbol.isLocal && symbol.isMethod) {
                        if (symbol.name == defDef.symbol.name) {
                            error(s"Nested local functions with same names aren't supported ($defDef).")
                        }
                        checkNameDuplicity(symbol.owner)
                    }
                }
                checkNameDuplicity(defDef.symbol.owner)

                val parameters = defDef.vparamss.flatten.map(p => localJsIdentifier(p.name))
                val body = unScoped(processReturnTree(defDef.rhs))
                js.FunctionDeclaration(localJsIdentifier(defDef.name), parameters, body)
            }
        }

        def processNew(n: New, args: Seq[Tree]): js.Expression = {
            js.NewExpression(js.CallExpression(typeJsIdentifier(n.tpe.underlying), processExpressionTrees(args)))
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
                    error(s"Unexpected type of a label ($labelDef)")
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
                val body = unScoped(processReturnTree(c.rhs))
                js.FunctionDeclaration(localJsIdentifier(c.name), c.params.map(processIdent _), body)
            }

            val firstCaseIdentifier = localJsIdentifier(matchBlock.cases.head.name)
            val matchResult = js.ReturnStatement(Some(js.CallExpression(firstCaseIdentifier, Nil)))

            scoped {
                (processedInit ++ processedCases) :+ matchResult
            }
        }

        def processThrow(t: Throw): js.Expression = scoped {
            js.ThrowStatement(processExpressionTree(t.expr))
        }

        def processTry(t: Try): js.Expression = t match {
            case Try(b, Nil, EmptyTree) => processExpressionTree(b)
            case _ => {
                val processedBody = unScoped(processReturnTree(t.block))
                val processedCatches = t.catches match {
                    case Nil => None

                    // If the cases contain some more advanced patterns than simple type check, wildcard or bind, then
                    // the patmat transforms the cases the same way as it transforms the match expression and wraps the
                    // match into one case. Therefore we don't have take care of exception rethrowing (in case of
                    // unsuccessful match) since it's already done in the case produced by patmat.
                    case List(CaseDef(Bind(matcheeName, _), _, b: Block)) if b.toMatchBlock.nonEmpty => {
                        Some((localJsIdentifier(matcheeName), unScoped(processReturnTree(b))))
                    }
                    case catches => {
                        val exception = freshLocalJsIdentifier("e")
                        val body = catches.flatMap(processCaseDef(_, exception)) :+ js.ThrowStatement(exception)
                        Some((exception, body))
                    }
                }
                val processedFinalizer =
                    if (t.finalizer == EmptyTree) None else Some(unScoped(processStatementTree(t.finalizer)))

                scoped {
                    js.TryStatement(processedBody, processedCatches, processedFinalizer)
                }
            }
        }

        def processCaseDef(caseDef: CaseDef, matchee: js.Expression): Seq[js.Statement] = {
            // The body is terminated with the return statement, so even if the body doesn't return anything, the
            // matching process is terminated.
            val processedBody = unScoped(processReturnTree(caseDef.body)) :+ js.ReturnStatement(None)
            val guardedBody = caseDef.guard match {
                case EmptyTree => processedBody
                case guard => List(js.IfStatement(processExpressionTree(guard), processedBody, Nil))
            }

            processPattern(caseDef.pat, matchee, guardedBody)
        }

        def processPattern(p: Tree, matchee: js.Expression, body: Seq[js.Statement]): Seq[js.Statement] = p match {
            case i: Ident => processIdentifierPattern(i, matchee, body)
            case t: Typed => processTypedPattern(t, matchee, body)
            case b: Bind => processBindPattern(b, matchee, body)
            case pattern => {
                error(s"Unexpected type of a pattern ($pattern).")
                Nil
            }
        }

        def processIdentifierPattern(identifier: Ident, matchee: js.Expression, body: Seq[js.Statement]) = {
            if (identifier.name != nme.WILDCARD) {
                error(s"Unexpected type of an identifier pattern ($identifier).")
            }
            body
        }

        def processTypedPattern(typed: Typed, matchee: js.Expression, body: Seq[js.Statement]) = {
            val condition = swatMethodCall(localIdentifier("isInstanceOf"), matchee, typeJsIdentifier(typed.tpt.tpe))
            List(js.IfStatement(condition, body, Nil))
        }

        def processBindPattern(bind: Bind, matchee: js.Expression, body: Seq[js.Statement]) = {
            val binding = js.VariableStatement(localJsIdentifier(bind.name), Some(matchee))
            processPattern(bind.body, matchee, binding +: body)
        }
    }

    private class ClassProcessor(c: ClassDef) extends ClassDefProcessor(c)
    private class TraitProcessor(c: ClassDef) extends ClassDefProcessor(c)
    private class ObjectProcessor(c: ClassDef) extends ClassDefProcessor(c)
    private class PackageObjectProcessor(c: ClassDef) extends ClassDefProcessor(c)
}
