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
                error("The %s must contain a package definition.".format(compilationUnit.source.file.name))
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
                val processor = ClassDefProcessor(classSymbol.classSymbolKind)
                processor.process(classDef)
            }

        js.Program(provide ++ statements)
    }


    def objectMethodCall(objectSymbol: Symbol, methodSymbol: Symbol, args: Seq[js.Expression]): js.Expression = {
        objectMethodCall(objectSymbol, localIdentifier(methodSymbol.name), args)
    }

    def objectMethodCall(objectSymbol: Symbol, methodName: String, args: Seq[js.Expression]): js.Expression = {
        // TODO
        methodCall(js.RawCodeExpression(objectSymbol.fullName), methodName, args: _*)
    }


    def swatMethodCall(methodName: String, args: js.Expression*): js.Expression = {
        methodCall(localJsIdentifier("swat"), methodName, args: _*) // TODO swat object.
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

    private def symbolIdentifier(symbol: Symbol): String = {
        if (symbol.owner.isPackageClass) {
            val qualifier = packageIdentifier(symbol.owner) match {
                case "" => ""
                case i => i + "."
            }
            qualifier + localIdentifier(symbol.name)
        } else {
            typeIdentifier(symbol.owner.tpe) + "$" + symbol.name.toString
        }
    }

    def localIdentifier(name: Name): String = localIdentifier(name.toString)
    def localIdentifier(name: String): String = (if (js.Language.keywords.contains(name)) "$" else "") + name
    def localJsIdentifier(name: Name): js.Identifier = localJsIdentifier(name.toString)
    def localJsIdentifier(name: String): js.Identifier = js.Identifier(localIdentifier(name))
}
