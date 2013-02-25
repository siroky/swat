package swat.compiler.frontend

import swat.compiler.{SwatCompilerPlugin, js}

trait ScalaAstProcessor
    extends js.TreeBuilder
    with RichTrees
    with ClassDefProcessors {
    self: SwatCompilerPlugin =>

    import global._

    /**
     * A list of packages that are stripped from the compiled JavaScript code. For example a class
     * swat.runtime.client.foo.bar.A is compiled to foo.bar.A. The main purpose is an easy integration of existing
     * libraries into the client code without naming collisions. This can be seen on the swat.runtime.client.scala
     * package where altered versions of Scala Library classes may be defined. Advantage is, that in the compiled
     * code, they seem like they were declared in the scala package.
     */
    val ignoredPackages = List("<root>", "<empty>", "swat.runtime.client")

    def processUnitBody(body: Tree): Map[String, js.Program] = body match {
        case p: PackageDef => {
            extractClassDefs(p).map(c => (typeIdentifier(c.symbol), processClassDef(c))).toMap
        }
        case _ => Map.empty
    }

    def extractClassDefs(tree: Tree): List[ClassDef] = tree match {
        case p: PackageDef => p.stats.flatMap(extractClassDefs _)
        case c: ClassDef if c.symbol.isCompiled => c :: c.impl.body.flatMap(extractClassDefs _)
        case _ => Nil
    }

    def processClassDef(classDef: ClassDef): js.Program = {
        val classSymbol = classDef.symbol
        val classType = classSymbol.tpe.underlying

        val provide = if (classSymbol.isLocalOrAnonymous) Nil else List(processProvide(classType))
        val statements =
            classSymbol.nativeAnnotation.map { code =>
                val requirements = classSymbol.dependencyAnnotations.map((processDependency _).tupled)
                val declarations = List(js.RawCodeBlock(code))
                requirements ++ declarations
            }.getOrElse {
                ClassDefProcessor(classDef).process()
            }

        js.Program(provide ++ statements)
    }

    def objectAccessor(objectSymbol: Symbol): js.Expression = objectAccessor(typeJsIdentifier(objectSymbol))
    def objectAccessor(objectExpression: js.Expression): js.Expression = js.CallExpression(objectExpression, Nil)

    def swatMethodCall(methodName: String, args: js.Expression*): js.Expression =
        methodCall(localJsIdentifier("swat"), localJsIdentifier(methodName), args: _*)

    def processDependency(dependencyType: Type, isHard: Boolean): js.Statement =
        js.ExpressionStatement(swatMethodCall(
            "require",
            js.StringLiteral(typeIdentifier(dependencyType)),
            js.BooleanLiteral(isHard)))

    def processProvide(dependencyType: Type): js.Statement =
        js.ExpressionStatement(swatMethodCall("provide", js.StringLiteral(typeIdentifier(dependencyType))))

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
        if (ignoredPackages.contains(packageSymbol.fullName)) {
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
            } else if (symbol.isLocalOrAnonymous) {
                localIdentifier(symbol.name)
            } else if (symbol.classSymbolKind == PackageObjectSymbol) {
                packageIdentifier(symbol.owner) // The $package suffix is stripped.
            } else if (symbol.owner.isPackageClass) {
                separateNonEmptyPrefix(packageIdentifier(symbol.owner), localIdentifier(symbol.name))
            } else {
                typeIdentifier(symbol.owner.tpe) + "$" + symbol.name.toString
            }

        val suffix = symbol.classSymbolKind match {
            case PackageObjectSymbol | ObjectSymbol => "$"
            case _ => ""
        }

        identifier + suffix
    }
    def typeIdentifier(tpe: Type): String = typeIdentifier(tpe.typeSymbol)
    def typeJsIdentifier(tpe: Type): js.Identifier = typeJsIdentifier(tpe.typeSymbol)
    def typeJsIdentifier(symbol: Symbol): js.Identifier = js.Identifier(typeIdentifier(symbol))

    private def separateNonEmptyPrefix(prefix: String, suffix: String, separator: String = ".") = prefix match {
        case "" => suffix
        case _ => prefix + separator + suffix
    }
}
