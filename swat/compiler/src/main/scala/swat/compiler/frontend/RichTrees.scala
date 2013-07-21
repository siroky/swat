package swat.compiler.frontend

import swat.compiler.{SwatCompilerPlugin, CompilationException}

trait RichTrees {
    self: SwatCompilerPlugin with ScalaAstProcessor with ClassDefProcessors =>

    import global._

    implicit class RichType(t: Type) {
        import definitions._

        val s = Option(t.underlying).getOrElse(t).typeSymbol

        def isUnit = t.underlying =:= typeOf[Unit]
        def isAny = t.underlying =:= typeOf[Any]
        def isPrimitiveOrString = isPrimitive || isString
        def isPrimitive = isNumericVal || isBoolean || isChar
        def isString = t.underlying =:= typeOf[String]
        def isNumericVal = s.isNumericValueClass
        def isIntegralVal = ScalaNumericValueClasses.filterNot(Set(FloatClass, DoubleClass)).contains(s)
        def isChar = t.underlying =:= typeOf[Char]
        def isBoolean = t.underlying =:= typeOf[Boolean]
        def isFunction = isFunctionType(t.underlying)
        def isArray = t.underlying <:< typeOf[Array[_]]
        def companionSymbol = s.companionSymbol
    }

    implicit class RichSymbol(s: Symbol) {
        def isObject = s.isPackageObjectOrClass || s.isModuleOrModuleClass
        def isLocalOrAnonymous = s.owner.isMethod || s.owner.isLocal || s.isAnonymous
        def isAnonymous: Boolean = s.isAnonymousClass || s.isAnonymousFunction || s.owner.isAnonymousClass

        def isField = Set("field", "value", "lazy value")(s.accurateKindString)
        def isParametricField = isField && !s.hasGetter && !s.isLazy && !s.isStatic

        def isCompiled = !(isIgnored || isRemote || isAdapter)
        def isIgnored = hasAnnotation(typeOf[swat.ignored])
        def isRemote = hasAnnotation(typeOf[swat.remote])

        def isAdapter = {
            hasAnnotation(typeOf[swat.adapter]) || adapterPackages.exists(p => s.fullName.startsWith(p) || p.startsWith(s.fullName))
        }

        def adapterAnnotation: Option[Boolean] = typedAnnotation(typeOf[swat.adapter]).map { s =>
            s.constantAtIndex(0).map(_.booleanValue).getOrElse(true)
        }

        def hasAnnotation(tpe: Type) = typedAnnotation(tpe).nonEmpty
        def typedAnnotation(tpe: Type) = typedAnnotations(tpe).headOption
        def typedAnnotations(tpe: Type) = s.annotations.filter(_.atp =:= tpe)

        def isApplyMethod = s.isMethod && s.nameString == "apply"

        def isEqualityOperator = s.isMethod && Set("==", "!=", "equals", "eq", "ne").contains(s.nameString)

        def isAnyMethodOrOperator = {
            val methods = Set("toString", "hashCode", "##", "asInstanceOf", "isInstanceOf", "getClass", "clone")
            isEqualityOperator || isTypeSpecificMethod(methods, _ => true)
        }

        def isPrimitiveOrStringOperator = isPrimitiveOperator || isStringOperator

        def isPrimitiveOperator = isNumericValOperator || isBooleanValOperator

        def isNumericValOperator = {
            val unaryArithmetic = Set("unary_+", "unary_-")
            val arithmetic = Set("+", "-", "*", "/", "%")
            val unaryBitwise = Set("unary_~")
            val bitwise = Set("&", "|", "^", "<<", ">>", ">>>")
            val relational = Set(">", "<", ">=", "<=")
            val operators = unaryArithmetic ++ arithmetic ++ relational ++ unaryBitwise ++ bitwise
            isTypeSpecificMethod(operators, _.isNumericVal)
        }

        def isBooleanValOperator = {
            val unaryLogical = Set("unary_!")
            val logicalShortCircuit = Set("&&", "||")
            val logicalLongCircuit = Set("&", "|", "^")
            isTypeSpecificMethod(unaryLogical ++ logicalShortCircuit ++ logicalLongCircuit, _.isBoolean)
        }

        def isStringOperator = isTypeSpecificMethod(Set("+"), _.isString)

        def isAnonymousTotalFunction = s.isAnonymousFunction && !(s.tpe <:< typeOf[PartialFunction[_, _]])

        def isRemoteMethod = s.isMethod && s.owner.isObject && (isRemote || s.owner.isRemote)

        private def isTypeSpecificMethod(methods: Set[String], typeFilter: Type => Boolean): Boolean = {
            s.isMethod && typeFilter(s.owner.tpe) && methods.contains(s.nameString)
        }
    }

    implicit class RichBlock(b: Block) {
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

    case class MatchBlock(init: List[Tree], cases: List[LabelDef])

    implicit class RichLabelDef(l: LabelDef) {
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
                        error(s"Unknown format of a while loop label ($l)")
                        (EmptyTree, Nil)
                    }
                }
                Some(Loop(expr, stats, isDoWhile))

            } else {
                None
            }
        }
    }

    case class Loop(expr: Tree, stats: List[Tree], isDoWhile: Boolean)

    implicit class RichClassDef(classDef: ClassDef) {
        def valDefs = classDef.impl.body.collect { case v: ValDef => v }
        def defDefs = classDef.impl.body.collect { case d: DefDef => d }
    }
}
