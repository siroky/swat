package swat.compiler.frontend

import swat.compiler.{SwatCompilerPlugin, js}

trait ScalaAstProcessor extends js.TreeBuilder with RichTrees with ClassDefProcessors {
    self: SwatCompilerPlugin =>
    import global._

    case class Dependency(tpe: Either[String, Type], isHard: Boolean)

    /**
     * A set of packages that are stripped from the compiled JavaScript code. For example a class
     * swat.client.foo.bar.A is compiled to foo.bar.A. The main purpose is an easy integration of existing
     * libraries into the client code without naming collisions. This can be seen on the swat.client.scala
     * package where altered versions of Scala Library classes may be defined. Advantage is, that in the compiled
     * code, they seem like they were declared in the scala package.
     */
    val ignoredPackages = Set("<root>", "<empty>", "swat.library", "swat.common", "swat.client", "swat.tests")

    /**
     * A set of packages whose classes and objects are considered to be adapters even though they don't necessarily
     * need to be annotated with the [[swat.adapter]] annotation.
     */
    val adapterPackages = Set("swat.js")

    def processUnitBody(body: Tree): Map[String, js.Program] = body match {
        case p: PackageDef => {
            extractClassDefs(p).map { classDef =>
                val classSymbol = classDef.symbol
                val classType = classSymbol.tpe.underlying
                val classIdent = typeIdentifier(classSymbol)
                val (dependencies, statements) = processClassDef(classDef)
                val processedProvide = processProvide(classType)
                val processedDependencies = processDependencies(dependencies, classIdent)
                val program = js.Program(processedProvide :: processedDependencies ++ statements)
                (classIdent, program)
            }.toMap
        }
        case _ => Map.empty
    }

    def extractClassDefs(tree: Tree): List[ClassDef] = tree match {
        case p: PackageDef => p.stats.flatMap(extractClassDefs _)
        case c: ClassDef if c.symbol.isCompiled => c :: c.impl.body.flatMap(extractClassDefs _)
        case _ => Nil
    }

    def processClassDef(classDef: ClassDef): (Seq[Dependency], List[js.Statement]) = {
        val classSymbol = classDef.symbol
        classSymbol.jsAnnotation.map { code =>
            (classSymbol.dependencyAnnotations, List(js.RawCodeBlock(code)))
        }.getOrElse {
            ClassDefProcessor(classDef).process()
        }
    }

    def processProvide(dependencyType: Type): js.Statement = {
        js.ExpressionStatement(swatMethodCall("provide", js.StringLiteral(typeIdentifier(dependencyType))))
    }

    def processDependencies(dependencies: Seq[Dependency], excludedTypeIdent: String): List[js.Statement] = {
        // Filter the dependencies so only relevant are included and map the first components to type identifiers.
        val filtered = dependencies.flatMap {
            case Dependency(Left(identifier), isHard) => Some(identifier, isHard)
            case Dependency(Right(tpe), isHard) => {
                if (!tpe.typeSymbol.isAdapter && !tpe.typeSymbol.isAnonymousClass) {
                    Some(typeIdentifier(tpe), isHard)
                } else {
                    None
                }
            }
        }

        // Group them by type identifiers and for each dependent type, use the strongest dependency
        // (i.e. declaration dependency).
        val grouped = filtered.groupBy(_._1) - excludedTypeIdent
        val strongest = grouped.mapValues(_.map(_._2).reduce(_ || _)).toList.sortBy(_._1)

        // Produce the swat.require statements.
        strongest.map { case (typeIdentifier, isHard) =>
            val expr = swatMethodCall("require", js.StringLiteral(typeIdentifier), js.BooleanLiteral(isHard))
            js.ExpressionStatement(expr)
        }
    }

    def objectAccessor(objectSymbol: Symbol): js.Expression = {
        if (objectSymbol.isAdapter) {
            typeJsIdentifier(objectSymbol)
        } else {
            js.CallExpression(typeJsIdentifier(objectSymbol), Nil)
        }
    }

    def swatMethodCall(methodName: String, args: js.Expression*): js.Expression =
        methodCall(localJsIdentifier("swat"), localJsIdentifier(methodName), args: _*)

    def localIdentifier(name: String): String = {
        val cleanName = name.replace(" ", "").replace("<", "$").replace(">", "$")
        (if (js.Language.keywords(cleanName)) "$" else "") + cleanName
    }
    def localIdentifier(name: Name): String = localIdentifier(name.toString)
    def localJsIdentifier(name: Name): js.Identifier = localJsIdentifier(name.toString)
    def localJsIdentifier(name: String): js.Identifier = js.Identifier(localIdentifier(name))

    private var counter = 0
    def freshLocalJsIdentifier(prefix: String) = {
        counter += 1
        js.Identifier(prefix + "$" + counter)
    }

    def packageIdentifier(packageSymbol: Symbol): String = {
        if (ignoredPackages(packageSymbol.fullName) || adapterPackages(packageSymbol.fullName)) {
            ""
        } else {
            separateNonEmptyPrefix(packageIdentifier(packageSymbol.owner), localIdentifier(packageSymbol.name))
        }
    }
    def packageJsIdentifier(packageSymbol: Symbol) = js.Identifier(packageIdentifier(packageSymbol))

    def typeIdentifier(symbol: Symbol): String = {
        val identifier =
            if (symbol == NoSymbol) {
                ""
            } else if (symbol.isTypeParameterOrSkolem || symbol.isLocalOrAnonymous) {
                localIdentifier(symbol.name)
            } else if (symbol.isAdapter) {
                val isScope = symbol.tpe <:< typeOf[swat.js.Scope]
                val isAdapterPackageObject = symbol.isPackageObjectOrClass && adapterPackages(symbol.owner.fullName)
                val stripPackage = symbol.adapterAnnotation.getOrElse(true)
                if (isScope || (isAdapterPackageObject && stripPackage)) {
                    ""
                } else {
                    val prefix = if (stripPackage) "" else packageIdentifier(symbol.owner)
                    separateNonEmptyPrefix(prefix, localIdentifier(symbol.name))
                }
            } else if (symbol.isPackageObjectOrClass) {
                packageIdentifier(symbol.owner)
            } else if (symbol.owner.isPackageClass) {
                separateNonEmptyPrefix(packageIdentifier(symbol.owner), localIdentifier(symbol.name))
            } else {
                typeIdentifier(symbol.owner.tpe) + "$" + symbol.name.toString
            }
        val suffix = if (symbol.isObject && !symbol.isAdapter) "$" else ""

        identifier + suffix
    }
    def typeIdentifier(tpe: Type): String = typeIdentifier(tpe.typeSymbol)
    def typeJsIdentifier(tpe: Type): js.Identifier = typeJsIdentifier(tpe.typeSymbol)
    def typeJsIdentifier(symbol: Symbol): js.Identifier = js.Identifier(typeIdentifier(symbol))

    def separateNonEmptyPrefix(prefix: String, suffix: String, separator: String = ".") = prefix match {
        case "" => suffix
        case _ => prefix + separator + suffix
    }
}
