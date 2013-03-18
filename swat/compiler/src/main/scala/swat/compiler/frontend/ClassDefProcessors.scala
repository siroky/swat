package swat.compiler.frontend

import swat.compiler.SwatCompilerPlugin
import swat.compiler.js
import collection.mutable

trait ClassDefProcessors {
    self: SwatCompilerPlugin with ScalaAstProcessor =>

    import global._

    object ClassDefProcessor {
        def apply(classDef: ClassDef): ClassDefProcessor = {
            val symbol = classDef.symbol
            if (symbol.isPackageObjectOrClass) {
                new PackageObjectProcessor(classDef)
            } else if (symbol.isModuleOrModuleClass) {
                new ObjectProcessor(classDef)
            } else if (symbol.isTrait) {
                new TraitProcessor(classDef)
            } else {
                new ClassProcessor(classDef)
            }
        }
    }

    class ClassDefProcessor(protected val classDef: ClassDef) {

        val dependencies = mutable.ListBuffer.empty[(Type, Boolean)]

        val thisTypeIdentifier = typeIdentifier(classDef.symbol.tpe)
        val thisTypeJsIdentifier = js.Identifier(thisTypeIdentifier)
        val thisTypeString = js.StringLiteral(thisTypeIdentifier)

        val selfIdent = localJsIdentifier("$self")
        val outerIdent = localJsIdentifier("$outer")
        val superIdent = localJsIdentifier("$super")
        val fieldsIdent = localJsIdentifier("$fields")
        val constructorIdent = localJsIdentifier("$init$")
        val selfDeclaration = js.VariableStatement(selfIdent, Some(js.ThisReference))

        def addDeclarationDependency(tpe: Type) {
            dependencies += tpe -> true
        }

        def addRuntimeDependency(tpe: Type) {
            dependencies += tpe -> false
        }

        def process(): (Dependencies, List[js.Statement]) = {
            // Group the methods by their names and process all the methods in a group together (merge overloaded
            // methods into a single method. In its body, it determines by parameter types which one of the overloaded
            // methods should be invoked).
            val defDefGroups = classDef.defDefs.groupBy(_.name.toString).toList.sortBy(_._1).map(_._2)
            val (constructorGroups, methodGroups) = defDefGroups.partition(_.head.symbol.isConstructor)

            // Process the single constructor group.
            val constructorDeclaration = processConstructorGroup(constructorGroups.headOption).toList

            // Process the method group.
            val methodGroupsToProcess = methodGroups.filter(!_.head.symbol.isOuterAccessor)
            val methodDeclarations = methodGroupsToProcess.map(processMethodGroup _)

            // The JavaScript constructor function.
            val superClasses = classDef.symbol.baseClasses
            val superClassIdentifiers = superClasses.map(c => typeJsIdentifier(c.tpe))
            val jsConstructorDeclaration = processJsConstructor(js.ArrayLiteral(superClassIdentifiers))
            superClasses.foreach(c => addDeclarationDependency(c.tpe))

            // Return the dependencies of the class and all statements.
            (dependencies, constructorDeclaration ++ methodDeclarations :+ jsConstructorDeclaration)
        }

        def processConstructorGroup(constructors: Option[List[DefDef]]): Option[js.Statement] = {
            val constructorExpression = constructors match {
                case Some(List(constructor)) => Some(processConstructor(constructor))
                case Some(cs) => Some(processDefDefGroup(cs, processConstructor _))
                case _ => None
            }
            val qualifier = memberChain(thisTypeJsIdentifier, constructorIdent)
            constructorExpression.map(e => js.AssignmentStatement(qualifier, e))
        }

        def processMethodGroup(methods: List[DefDef]): js.Statement = {
            val methodExpression = processDefDefGroup(methods, processDefDef(_: DefDef))
            val qualifier = memberChain(thisTypeJsIdentifier, localJsIdentifier(methods.head.name))
            js.AssignmentStatement(qualifier, methodExpression)
        }

        def processDefDefGroup(defDefs: List[DefDef], defDefProcessor: DefDef => js.Expression): js.Expression = {
            // Each method is processed and a type hint containing types of the method formal parameters is
            // added. E.g. [Int], function(i) { ... },  [String, String], function(s1, s2) { ... }.
            val overloads = defDefs.flatMap { defDef =>
                val parameterTypes = defDef.vparamss.flatten.map(p => p.tpt.tpe)
                parameterTypes.foreach(addRuntimeDependency _)
                val parameterIdents = parameterTypes.map(typeIdentifier _)
                List(js.StringLiteral(parameterIdents.mkString(", ")), defDefProcessor(defDef))
            }
            val methodIdentifier = js.StringLiteral(thisTypeIdentifier + "." + localIdentifier(defDefs.head.name))
            swatMethodCall("method", (methodIdentifier :: overloads): _*)
        }

        def processConstructor(c: DefDef): js.Expression = {
            if (c.symbol.isPrimaryConstructor) {
                processPrimaryConstructor(c)
            } else {
                processDefDef(c)
            }
        }

        def processPrimaryConstructor(c: DefDef): js.Expression = {
            val processedConstructor = processDefDef(c)

            // The self decalaration, super constructor call.
            val bodyStart = processedConstructor.body match {
                case List(s, js.ExpressionStatement(js.UndefinedLiteral)) => {
                    // The body doesn't contain a call to super constructor, so it has to be added.
                    List(s, js.ExpressionStatement(superCall(None, constructorIdent.name, Nil)))
                }
                case b => b
            }

            // Initialization of vals, vars and constructor parameters.
            val fields = classDef.valDefs.filter(!_.symbol.isLazy)
            val parameterIdentifiers = c.vparamss.flatten.map(p => localJsIdentifier(p.symbol.name))
            val fieldInitialization = fields.map { f =>
                val parameter = localJsIdentifier(f.symbol.name)
                val value = processExpressionTree(f.rhs)
                fieldSet(f.symbol, if (parameterIdentifiers.contains(parameter)) parameter else value)
            }

            // Initialization of lazy vals.
            val lazyFieldInitialization = classDef.defDefs.filter(_.symbol.isLazy).map { defDef: DefDef =>
                val value = defDef.rhs match {
                    // Classes and objects have the following structure of lazy val getter.
                    case Block(List(Assign(_, rhs), _*), _) => rhs
                    case _ => defDef.rhs
                }
                fieldSet(defDef.symbol, value)
            }

            // Rest of the constructor body.
            val bodyStatements = processStatementTrees(classDef.impl.body.filter(!_.isDef))
            val body = bodyStart ++ fieldInitialization ++ lazyFieldInitialization ++ bodyStatements
            js.FunctionExpression (None, processedConstructor.parameters, body)
        }

        def processDefDef(defDef: DefDef, includeSelf: Boolean = true): js.FunctionExpression = {
            val processedParameters = defDef.vparamss.flatten.map(p => localJsIdentifier(p.name))
            val self = if (includeSelf) List(selfDeclaration) else Nil
            val processedBody =
                if (defDef.symbol.isGetter && defDef.symbol.isLazy) {
                    // Body of a lazy val (which is assigned to the corresponding field in the primary constructor)
                    // can be replaced by simple return of the field, where the lazy val is stored.
                    js.ReturnStatement(Some(fieldGet(defDef.symbol)))
                } else {
                    processReturnTree(defDef.rhs, defDef.tpt.tpe)
                }
            js.FunctionExpression(None, processedParameters, self ++ unScoped(processedBody))
        }

        def processJsConstructor(superClasses: js.ArrayLiteral): js.Statement = {
            js.AssignmentStatement(thisTypeJsIdentifier, swatMethodCall("type", thisTypeString, superClasses))
        }

        def symbolToField(qualifier: js.Expression, symbol: Symbol): js.Expression = {
            memberChain(qualifier, fieldsIdent, localJsIdentifier(symbol.name))
        }

        def processTree(tree: Tree): js.Ast = tree match {
            case EmptyTree => js.UndefinedLiteral
            case b: Block => processBlock(b)
            case l: Literal => processLiteral(l)
            case t: TypeTree => processTypeTree(t)
            case a: ArrayValue => processArrayValue(a)
            case i: Ident => processIdent(i)
            case t: This => processThis(t.symbol)
            case s: Super => processSuper(s)
            case s: Select => processSelect(s)
            case a: Apply => processApply(a)
            case t: TypeApply => processTypeApply(t)
            case t: Typed => processTyped(t)
            case a: Assign => processAssign(a)
            case v: ValDef => processLocalValDef(v)
            case d: DefDef => processLocalDefDef(d)
            case c: ClassDef => processLocalClassDef(c)
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

        def processReturnTree(tree: Tree, returnTpe: Type = null): js.Statement = {
            // If the type of the tree is Unit, then the tree appears on the return position of an expression, which
            // actually doesn't return anything. So the 'return' may be omitted.
            if (Option(returnTpe).getOrElse(tree.tpe).isUnit) {
                tree match {
                    // If the tree is a Block with structure { statement; (); } then the block that wraps the statement
                    // may be omitted. The scope protects from shadowing and using the shadowed value instead of the
                    // original value. However it's not possible to shadow and use a variable in one statement, which
                    // isn't itself scoped. The purpose is to get rid of unnecessary scoping.
                    case Block(statement :: Nil, Literal(Constant(_: Unit))) => processStatementTree(statement)
                    case _ => processStatementTree(tree)
                }
            } else {
                js.ReturnStatement(Some(processExpressionTree(tree)))
            }
        }

        def processStatementTrees(trees: List[Tree]): List[js.Statement] = trees.map(processStatementTree _)

        def processExpressionTrees(trees: List[Tree]): List[js.Expression] = trees.map(processExpressionTree _)

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
                addRuntimeDependency(t)
                swatMethodCall("classOf", typeJsIdentifier(t))
            }
            case l => {
                error(s"Unexpected type of a literal ($l)")
                js.UndefinedLiteral
            }
        }

        def processTypeTree(typeTree: TypeTree): js.Expression = {
            val tpe = typeTree.tpe.underlying
            addRuntimeDependency(tpe)
            typeJsIdentifier(tpe)
        }

        def processArray(values: List[Tree]): js.Expression = js.ArrayLiteral(processExpressionTrees(values))

        def processArrayValue(arrayValue: ArrayValue): js.Expression = processArray(arrayValue.elems)

        def processAnonymousFunction(functionClassDef: ClassDef): js.Expression = {
            val applyDefDef = functionClassDef.defDefs.filter(_.symbol.isApplyMethod).head
            processDefDef(applyDefDef, includeSelf = false)
        }

        def processIdent(identifier: Ident): js.Expression = {
            if (identifier.symbol.isModule) {
                addRuntimeDependency(identifier.tpe)
                objectAccessor(identifier.symbol)
            } else {
                localJsIdentifier(identifier.name)
            }
        }

        def processThis(thisSymbol: Symbol): js.Expression = {
            if (thisSymbol.isPackageClass) {
                packageJsIdentifier(thisSymbol)
            } else {
                // The thisSymbol isn't a package, therefore it's the current class or an outer class.
                def getNestingDepth(innerClass: Symbol): Int = {
                    if (innerClass == thisSymbol) 0 else getNestingDepth(innerClass.owner) + 1
                }
                val depth = getNestingDepth(classDef.symbol)
                (1 to depth).foldLeft[js.Expression](selfIdent)((z, _) => js.MemberExpression(z, outerIdent))
            }
        }

        def processSuper(s: Super): js.Expression = {
            js.CallExpression(memberChain(processExpressionTree(s.qual), superIdent), Nil)
        }

        def processSelect(select: Select): js.Expression = {
            val processedSelect = memberChain(processExpressionTree(select.qualifier), localJsIdentifier(select.name))
            select.symbol match {
                case s if s.isObject => {
                    addRuntimeDependency(s.tpe)
                    objectAccessor(s)
                }

                // A method invocation without the corresponding apply.
                case m: MethodSymbol => js.CallExpression(processedSelect, Nil)
                case s if s.isField => fieldGet(s)
                case _ => processedSelect
            }
        }

        /**
         * Processes an Apply AST. May return both Expression or Statement depending on the application type, because
         * some applications lead to an assignment statement.
         */
        def processApply(apply: Apply) = apply.fun match {
            // Outer field getter.
            case s: Select if s.symbol.isOuterAccessor => memberChain(processExpressionTree(s.qualifier), outerIdent)

            // Generic method call.
            case TypeApply(f, typeArgs) => {
                typeArgs.foreach(a => addRuntimeDependency(a.tpe))
                processCall(f, apply.args ++ typeArgs)
            }

            // Constructor call.
            case Select(n: New, _) => processNew(apply, n)

            // A local object constructor call can be omitted because every object access is via an accessor.
            case f if f.symbol.isModule => {
                addRuntimeDependency(apply.tpe)
                objectAccessor(apply.tpe.typeSymbol)
            }

            // An application on an adapter.
            case s: Select if s.qualifier.tpe.typeSymbol.isAdapter => processAdapterApply(s, apply.args)

            // Method call.
            case f => processCall(f, apply.args)
        }

        def processAdapterApply(method: Select, args: List[Tree]) = {
            val symbol = method.symbol
            if (symbol.isAccessor) {
                val fieldName = localJsIdentifier(method.name.toString.stripSuffix("_$eq"))
                val field = js.MemberExpression(processExpressionTree(method.qualifier), fieldName)
                if (symbol.isGetter) {
                    field
                } else {
                    js.AssignmentStatement(field, processExpressionTree(args.head))
                }
            } else if (symbol.isApplyMethod) {
                functionCall(method.qualifier, args)
            } else {
                val methodExpr = memberChain(processExpressionTree(method.qualifier), localJsIdentifier(method.name))
                functionCall(methodExpr, args)
            }
        }

        def processCall(method: Tree, args: List[Tree]): js.Expression = method match {
            // A local function call doesn't need the type hint, because it can't be overloaded.
            case f if f.symbol.isLocal => functionCall(f, args)

            // Methods on types that compile to JavaScript built-in types (primitive, function, array).
            case s @ Select(q, _) if q.tpe.isAnyValOrString => processAnyValOrStringMethodCall(s.symbol, q, args)
            case s @ Select(q, _) if q.tpe.isFunction => processFunctionMethodCall(s.symbol, q, args)
            case s @ Select(q, _) if q.tpe.isArray => dispatchCallToCompanion(s.symbol, q, args)

            // Standard methods of the Any class.
            case s @ Select(q, _) if s.symbol.isAnyMethodOrOperator => processAnyMethodCall(s.symbol, q, args)

            // Methods of the current class super classes.
            case s @ Select(Super(t: This, mixName), methodName) if t.symbol.tpe =:= classDef.symbol.tpe => {
                val arguments = processMethodArgs(s.symbol, Some(s.qualifier), args)
                val mix = if (mixName.isEmpty) None else Some(mixName.toString)
                superCall(mix, localIdentifier(methodName), arguments)
            }

            // Method call.
            case s @ Select(qualifier, name) => {
                val methodExpr = memberChain(processExpressionTree(qualifier), localJsIdentifier(name))
                js.CallExpression(methodExpr, processMethodArgs(s.symbol, Some(qualifier), args))
            }
        }

        def functionCall(function: Tree, args: List[Tree]): js.Expression = {
            functionCall(processExpressionTree(function), args)
        }
        def functionCall(function: js.Expression, args: List[Tree]): js.Expression = {
            js.CallExpression(function, processExpressionTrees(args))
        }

        def processMethodArgs(method: Symbol, qualifier: Option[Tree], args: List[Tree]): List[js.Expression] =  {
            val methodSymbol = method.asInstanceOf[MethodSymbol]
            val methodType = qualifier.map(q => methodSymbol.typeAsMemberOf(q.symbol.tpe)).getOrElse(methodSymbol.tpe)
            val paramTypes = methodType.paramTypes

            val firstParam = method.paramss.flatten.headOption
            val firstParamIsOuter = firstParam.map(_.name.endsWith(nme.OUTER)).getOrElse(false)
            val hintTypes = if (firstParamIsOuter) paramTypes.tail else paramTypes
            val hintIdents = hintTypes.map(typeIdentifier _)
            val typeHint = if (hintTypes.isEmpty) None else Some(js.StringLiteral(hintIdents.mkString(", ")))

            processExpressionTrees(args) ++ typeHint.toList
        }

        def dispatchCallToCompanion(method: Symbol, qualifier: Tree, args: List[Tree]): js.Expression = {
            val processedArgs = processExpressionTree(qualifier) +: processMethodArgs(method, Some(qualifier), args)
            val companion = qualifier.tpe.companionSymbol
            addRuntimeDependency(companion.tpe)
            methodCall(objectAccessor(companion), localJsIdentifier(method.name), processedArgs: _*)
        }

        def processAnyValOrStringMethodCall(method: Symbol, qualifier: Tree, args: List[Tree]): js.Expression = {
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
            val operator = processOperator(symbol.nameString.stripPrefix("unary_"))

            // Chars, that are represented as strings, need to be explicitly converted to integers, so arithmetic
            // operations would work on them.
            def processOperand(operand: Tree): js.Expression = {
                val processedOperand = processExpressionTree(operand)
                if (!symbol.isEqualityOperator && operand.tpe.isChar) {
                    val charCompanion = (typeOf[Char].companionSymbol)
                    addRuntimeDependency(charCompanion.tpe)
                    methodCall(objectAccessor(charCompanion), localJsIdentifier("toInt"), processedOperand)
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
                    methodCall(js.Identifier("Math"), js.Identifier("floor"), expr)
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

        def processAnyMethodCall(method: Symbol, qualifier: Tree, args: List[Tree]): js.Expression = {
            val processedQualifier = processExpressionTree(qualifier)
            if (method.isEqualityOperator) {
                val processedOperand2 = processExpressionTree(args.head)
                val equalityExpr = swatMethodCall(localIdentifier("equals"), processedQualifier, processedOperand2)

                method.nameString match {
                    case "==" | "equals" => equalityExpr
                    case "!=" => js.PrefixExpression("!", equalityExpr)
                    case o => js.InfixExpression(processedQualifier, processOperator(o), processedOperand2)
                }
            } else {
                val methodName = method.nameString.replace("##", "hashCode")
                val processedArgs = processExpressionTree(qualifier) +: processMethodArgs(method, Some(qualifier), args)
                swatMethodCall(localIdentifier(methodName), processedArgs: _*)
            }
        }

        def processFunctionMethodCall(method: Symbol, qualifier: Tree, args: List[Tree]): js.Expression = {
            if (method.isApplyMethod) {
                functionCall(qualifier, args)
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

        def processAssign(assign: Assign): js.Statement = {
            js.AssignmentStatement(processExpressionTree(assign.lhs), processExpressionTree(assign.rhs))
        }

        def processLocalValDef(valDef: ValDef): js.Statement = valDef.symbol match {
            // A val definition associated with a lazy val can be omitted as the value will be stored in the
            // corresponding function (see processLocalDefDef method).
            case s if s.isLazy && s.name.endsWith("$lzy") => js.EmptyStatement

            // The val definition associated with a local object can be omitted.
            case s if s.isModuleVar => js.EmptyStatement

            case _ => js.VariableStatement(localJsIdentifier(valDef.name), Some(processExpressionTree(valDef.rhs)))
        }

        def processLazyVal(defDef: DefDef): js.Statement = defDef.rhs match {
            case Block(List(Assign(_, rhs)), _) => {
                js.VariableStatement(localJsIdentifier(defDef.name), Some(lazify(rhs)))
            }
            case _ => {
                error("Unexpected lazy val initializer (%s)".format(defDef.rhs))
                js.EmptyStatement
            }
        }

        def processLocalDefDef(defDef: DefDef): js.Statement = {
            if (defDef.symbol.isModule) {
                js.EmptyStatement
            } else if (defDef.symbol.isLazy) {
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

                js.VariableStatement(localJsIdentifier(defDef.name), Some(processDefDef(defDef, includeSelf = false)))
            }
        }

        def processLocalClassDef(classDef: ClassDef): js.Statement = {
            val (classDependencies, statements) = processClassDef(classDef)
            dependencies ++= classDependencies
            js.Block(statements)
        }

        def processNew(apply: Apply, n: New): js.Expression = {
            val tpe = n.tpe.underlying
            addRuntimeDependency(tpe)

            val constructors = tpe.members.toList.filter(c => c.isConstructor && c.owner == tpe.typeSymbol)
            val args =
                if (constructors.length > 1) {
                    // If the created class has more than one constructor, a type hint has to be added
                    processMethodArgs(apply.fun.symbol, None, apply.args)
                } else {
                    processExpressionTrees(apply.args)
                }
            js.NewExpression(js.CallExpression(typeJsIdentifier(n.tpe.underlying), args))
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
                js.FunctionDeclaration(localJsIdentifier(c.name), c.params.map(i => localJsIdentifier(i.name)), body)
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

        def processCaseDef(caseDef: CaseDef, matchee: js.Expression): List[js.Statement] = {
            // The body is terminated with the return statement, so even if the body doesn't return anything, the
            // matching process is terminated.
            val processedBody = unScoped(processReturnTree(caseDef.body)) :+ js.ReturnStatement(None)
            val guardedBody = caseDef.guard match {
                case EmptyTree => processedBody
                case guard => List(js.IfStatement(processExpressionTree(guard), processedBody, Nil))
            }

            processPattern(caseDef.pat, matchee, guardedBody)
        }

        def processPattern(p: Tree, matchee: js.Expression, body: List[js.Statement]): List[js.Statement] = p match {
            case i: Ident => processIdentifierPattern(i, matchee, body)
            case t: Typed => processTypedPattern(t, matchee, body)
            case b: Bind => processBindPattern(b, matchee, body)
            case pattern => {
                error(s"Unexpected type of a pattern ($pattern).")
                Nil
            }
        }

        def processIdentifierPattern(identifier: Ident, matchee: js.Expression, body: List[js.Statement]) = {
            if (identifier.name != nme.WILDCARD) {
                error(s"Unexpected type of an identifier pattern ($identifier).")
            }
            body
        }

        def processTypedPattern(typed: Typed, matchee: js.Expression, body: List[js.Statement]) = {
            val condition = swatMethodCall(localIdentifier("isInstanceOf"), matchee, typeJsIdentifier(typed.tpt.tpe))
            List(js.IfStatement(condition, body, Nil))
        }

        def processBindPattern(bind: Bind, matchee: js.Expression, body: List[js.Statement]) = {
            val binding = js.VariableStatement(localJsIdentifier(bind.name), Some(matchee))
            processPattern(bind.body, matchee, binding +: body)
        }

        def processOperator(operator: String): String = {
            Map("equals" -> "==", "eq" -> "===", "ne" -> "!==").withDefault(o => o)(operator)
        }

        def lazify(expr: Tree): js.Expression = {
            swatMethodCall("lazify", js.FunctionExpression(None, Nil, unScoped(processReturnTree(expr))))
        }

        def superCall(mixName: Option[String], methodName: String, args: List[js.Expression]): js.Expression = {
            val method = js.StringLiteral(methodName)
            val arguments = js.ArrayLiteral(args)
            val typeHints = thisTypeString :: mixName.map(js.StringLiteral(_)).toList
            swatMethodCall("invokeSuper", (selfIdent :: method :: arguments :: typeHints): _*)
        }

        def fieldGet(field: Symbol): js.Expression = {
            if (field.isParametricField) {
                val name = js.StringLiteral(localIdentifier(field.name))
                swatMethodCall("getParameter", selfIdent, name, thisTypeString)
            } else {
                // Val, var or lazy val.
                val value = symbolToField(selfIdent, field)
                if (field.isLazy) js.CallExpression(value, Nil) else value
            }
        }

        def fieldSet(field: Symbol, value: Tree): js.Statement = {
            fieldSet(field, if (field.isLazy) lazify(value) else processExpressionTree(value))
        }

        def fieldSet(field: Symbol, value: js.Expression): js.Statement = {
            if (field.isOuterAccessor) {
                js.AssignmentStatement(memberChain(selfIdent, outerIdent), value)
            } else if (field.isParametricField) {
                val name = js.StringLiteral(localIdentifier(field.name))
                js.ExpressionStatement(swatMethodCall("setParameter", selfIdent, name, value, thisTypeString))
            } else {
                // Val, var or lazy val.
                js.AssignmentStatement(symbolToField(selfIdent, field), value)
            }
        }
    }

    private class ClassProcessor(c: ClassDef) extends ClassDefProcessor(c)

    private class TraitProcessor(c: ClassDef) extends ClassDefProcessor(c)

    private class ObjectProcessor(c: ClassDef) extends ClassDefProcessor(c) {
        override def processJsConstructor(superClasses: js.ArrayLiteral): js.Statement = {
            // A local object depends on the outer class so a reference to the outer class has to be passed to the
            // constructor.
            val commonArgs = List(thisTypeString, superClasses)
            val args = commonArgs ++ (if (classDef.symbol.isLocalOrAnonymous) List(selfIdent) else Nil)
            js.AssignmentStatement(thisTypeJsIdentifier, swatMethodCall("object", args: _*))
        }
    }

    private class PackageObjectProcessor(c: ClassDef) extends ObjectProcessor(c)
}
