package swat.compiler.frontend

import swat.compiler.{SwatCompilerPlugin, js}

trait ScalaAstProcessor
    extends js.TreeBuilder
    with RichTrees
    with DefinitionProcessors
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
    val ignoredPackages = List("__root__", "<empty>", "swat.runtime.client")

    def processCompilationUnit(compilationUnit: CompilationUnit): Map[String, js.Program] = {
        compilationUnit.body match {
            case p: PackageDef => {
                extractDefinitions(p).map(d => (definitionIdentifier(d.symbol), processDefinition(d))).toMap
            }
            case _ => {
                error("The %s must contain a package definition.".format(compilationUnit.source.file.name))
                Map.empty
            }
        }
    }

    def extractDefinitions(tree: Tree): List[ClassDef] = tree match {
        case p: PackageDef => p.stats.flatMap(extractDefinitions _)
        case c: ClassDef if c.symbol.isCompiled => c :: c.impl.body.flatMap(extractDefinitions _)
        case _ => Nil
    }

    def processDefinition(definition: ClassDef): js.Program = {
        val defSymbol = definition.symbol

        val provide = List(processProvide(defSymbol.tpe))
        val statements =
            defSymbol.nativeAnnotation.map { code =>
                val requirements = defSymbol.dependencyAnnotations.map((processDependency _).tupled)
                val declarations = List(js.RawCodeBlock(code))
                requirements ++ declarations
            }.getOrElse {
                val processor = DefinitionProcessor(defSymbol.definitionType)
                processor.process(definition)
            }

        js.Program(provide ++ statements)
    }


    def objectMethodCall(objectSymbol: Symbol, methodName: String, args: js.Expression*): js.Expression = {
        // TODO
        methodCall(js.RawCodeExpression(objectSymbol.fullName), methodName, args: _*)
    }

    def swatMethodCall(methodName: String, args: js.Expression*): js.Expression = {
        methodCall(js.Identifier("swat"), methodName, args: _*) // TODO
    }

    def processDependency(dependencyType: Type, isHard: Boolean): js.Statement = {
        js.ExpressionStatement(swatMethodCall(
            "require",
            js.StringLiteral(definitionIdentifier(dependencyType.typeSymbol)),
            js.BooleanLiteral(isHard)))
    }

    def processProvide(dependencyType: Type): js.Statement = {
        js.ExpressionStatement(swatMethodCall(
            "provide",
            js.StringLiteral(definitionIdentifier(dependencyType.typeSymbol))))
    }

    def packageIdentifier(pkgSymbol: Symbol) = {
        require(pkgSymbol.isPackage)
        ignoredPackages.foldLeft(pkgSymbol.fullName)((z, p) => z.stripPrefix(p))
    }

    def definitionIdentifier(defSymbol: Symbol): String = {
        val qualifier =
            if (defSymbol.owner.isPackageClass) {
                (packageIdentifier(defSymbol.enclosingPackage) + ".").stripPrefix(".")
            } else {
                definitionIdentifier(defSymbol.owner) + "$"
            }
        val suffix = defSymbol.definitionType match {
            case PackageObjectDefinition | ObjectDefinition => "$"
            case _ => ""
        }

        qualifier + defSymbol.name + suffix
    }
}
