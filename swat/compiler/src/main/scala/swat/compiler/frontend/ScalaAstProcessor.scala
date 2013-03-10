package swat.compiler.frontend

import swat.compiler.{SwatCompilerPlugin, js}
import swat.api.js.{JSON, console, document, window}

trait ScalaAstProcessor extends js.TreeBuilder with RichTrees with ClassDefProcessors {
    self: SwatCompilerPlugin =>

    import global._

    type Dependencies = Seq[(Type, Boolean)]

    /**
     * A set of packages that are stripped from the compiled JavaScript code. For example a class
     * swat.runtime.client.foo.bar.A is compiled to foo.bar.A. The main purpose is an easy integration of existing
     * libraries into the client code without naming collisions. This can be seen on the swat.runtime.client.scala
     * package where altered versions of Scala Library classes may be defined. Advantage is, that in the compiled
     * code, they seem like they were declared in the scala package.
     */
    val ignoredPackages = Set("<root>", "<empty>", "swat.runtime.client")

    /**
     * A set of packages whose classes and objects are considered to be adapters even though they don't necessarily
     * need to be annotated with the [[swat.api.adapter]] annotation.
     */
    val adapterPackages = Set("swat.api.js")

    def processUnitBody(body: Tree): Map[String, js.Program] = body match {
        case p: PackageDef => {
            extractClassDefs(p).map { classDef =>
                val classSymbol = classDef.symbol
                val classType = classSymbol.tpe.underlying
                val classIdentifier = typeIdentifier(classSymbol)
                val (dependencies, statements) = processClassDef(classDef)
                val provide = processProvide(classType)
                val requires = processDependencies(dependencies, classIdentifier)
                val program = js.Program(provide :: requires ++ statements)
                (classIdentifier, program)
            }.toMap
        }
        case _ => Map.empty
    }

    def extractClassDefs(tree: Tree): List[ClassDef] = tree match {
        case p: PackageDef => p.stats.flatMap(extractClassDefs _)
        case c: ClassDef if c.symbol.isCompiled => c :: c.impl.body.flatMap(extractClassDefs _)
        case _ => Nil
    }

    def processClassDef(classDef: ClassDef): (Dependencies, List[js.Statement]) = {
        val classSymbol = classDef.symbol
        classSymbol.nativeAnnotation.map { code =>
            (classSymbol.dependencyAnnotations, List(js.RawCodeBlock(code)))
        }.getOrElse {
            ClassDefProcessor(classDef).process()
        }
    }

    def processProvide(dependencyType: Type): js.Statement = {
        js.ExpressionStatement(swatMethodCall("provide", js.StringLiteral(typeIdentifier(dependencyType))))
    }

    def processDependencies(dependencies: Dependencies, excludedTypeIdentifier: String): List[js.Statement] = {
        // Group the dependencies by their type identifiers.
        val grouped = dependencies.map(d => (typeIdentifier(d._1), d._2)).groupBy(_._1) - excludedTypeIdentifier

        // For each dependent type, use the strongest dependency (i.e. declaration dependency).
        val strongest = grouped.mapValues(_.map(_._2).reduce(_ || _)).toList.sortBy(_._1)

        // Produce the swat.require statements.
        strongest.map { case (typeIdentifier, isDeclaration) =>
            val expr = swatMethodCall("require", js.StringLiteral(typeIdentifier), js.BooleanLiteral(isDeclaration))
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
            } else if (symbol.isLocalOrAnonymous) {
                localIdentifier(symbol.name)
            } else if (symbol.isAdapter) {
                val stripPackage = symbol.adapterAnnotation.getOrElse(true)
                if (stripPackage && symbol.isPackageObjectOrClass && adapterPackages(symbol.owner.fullName)) {
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
