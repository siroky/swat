package swat.compiler.frontend

import swat.compiler.{SwatCompilerPlugin, js}

trait ScalaAstProcessor
    extends js.TreeBuilder
    with RichTrees
    with ClassDefProcessors
{
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

    def processCompilationUnit(compilationUnit: CompilationUnit): Map[String, js.Program] = {
        compilationUnit.body match {
            case p: PackageDef => {
                extractClassDefs(p).map(c => (typeIdentifier(c.symbol.tpe), processClassDef(c))).toMap
            }
            case _ => {
                val fileName = compilationUnit.source.file.name
                error(s"The source file $fileName must contain a package definition.")
                Map.empty
            }
        }
    }

    def extractClassDefs(tree: Tree): List[ClassDef] = tree match {
        case p: PackageDef => p.stats.flatMap(extractClassDefs _)
        case c: ClassDef if c.symbol.isCompiled => c :: c.impl.body.flatMap(extractClassDefs _)
        case _ => Nil
    }

    def processClassDef(classDef: ClassDef): js.Program = {
        val classSymbol = classDef.symbol

        val provide = List(processProvide(classSymbol.tpe.underlying))
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

    def objectMethodCall(objectSymbol: Symbol, methodName: String, args: List[js.Expression]): js.Expression = {
        objectMethodCall(objectSymbol, localJsIdentifier(methodName), args)
    }

    def objectMethodCall(objectSymbol: Symbol, methodSymbol: Symbol, args: List[js.Expression]): js.Expression = {
        objectMethodCall(objectSymbol, localJsIdentifier(methodSymbol.name), args)
    }

    def objectMethodCall(objectSymbol: Symbol, methodName: js.Identifier, args: List[js.Expression]) = {
        // TODO
        methodCall(js.RawCodeExpression(objectSymbol.fullName), methodName, args: _*)
    }

    def swatMethodCall(methodName: String, args: js.Expression*): js.Expression = {
        methodCall(localJsIdentifier("swat"), localJsIdentifier(methodName), args: _*) // TODO swat object.
    }

    def processDependency(dependencyType: Type, isHard: Boolean): js.Statement = {
        js.ExpressionStatement(swatMethodCall(
            "require",
            js.StringLiteral(typeIdentifier(dependencyType)),
            js.BooleanLiteral(isHard)))
    }

    def processProvide(dependencyType: Type): js.Statement = {
        js.ExpressionStatement(swatMethodCall("provide", js.StringLiteral(typeIdentifier(dependencyType))))
    }

    def packageIdentifier(packageSymbol: Symbol): String = {
        if (ignoredPackages.contains(packageSymbol.fullName)) {
            ""
        } else {
            symbolIdentifier(packageSymbol)
        }
    }

    def typeIdentifier(tpe: Type): String = {
        val suffix = tpe.typeSymbol.classSymbolKind match {
            case PackageObjectSymbol | ObjectSymbol => "$"
            case _ => ""
        }

        symbolIdentifier(tpe.typeSymbol) + suffix
    }

    def symbolIdentifier(symbol: Symbol): String = {
        if (symbol.classSymbolKind == PackageObjectSymbol) {
            // The $package suffix is stripped.
            symbolIdentifier(symbol.owner)
        } else if (symbol.owner.isPackageClass) {
            val qualifier = packageIdentifier(symbol.owner) match {
                case "" => ""
                case i => i + "."
            }
            qualifier + localIdentifier(symbol.name)
        } else {
            typeIdentifier(symbol.owner.tpe) + "$" + symbol.name.toString
        }
    }

    def localIdentifier(name: String): String = {
        val cleanName = name.replace(" ", "").replace("<", "$").replace(">", "$")
        (if (js.Language.keywords(cleanName)) "$" else "") + cleanName
    }

    private var counter = 0
    def freshLocalJsIdentifier(prefix: String) = {
        counter += 1
        js.Identifier(prefix + "$" + counter)
    }

    def localIdentifier(name: Name): String = localIdentifier(name.toString)
    def localJsIdentifier(name: Name): js.Identifier = localJsIdentifier(name.toString)
    def localJsIdentifier(name: String): js.Identifier = js.Identifier(localIdentifier(name))
    def symbolJsIdentifier(symbol: Symbol) = js.Identifier(symbolIdentifier(symbol))
    def typeJsIdentifier(tpe: Type) = js.Identifier(typeIdentifier(tpe))
    def packageJsIdentifier(packageSymbol: Symbol) = js.Identifier(packageIdentifier(packageSymbol))
}
