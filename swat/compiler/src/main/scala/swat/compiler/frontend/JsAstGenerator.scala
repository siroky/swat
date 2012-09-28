package swat.compiler.frontend

import tools.nsc.Global
import swat.compiler.js

trait JsAstGenerator
{
    self: {val global: Global} =>

    import global._

    def processCompilationUnit(compilationUnit: CompilationUnit): js.Program = {
        compilationUnit.body match {
            case p: PackageDef => {
                js.Program.empty
            }
            case _ => {
                error("The %s must contain a package definition.".format(compilationUnit.source.file.name))
                js.Program.empty
            }
        }
    }

    private def extractClassDefs(tree: Tree): List[ClassDef] = {
        def extract(t: Tree): List[ClassDef] = {
            t match {
                case p: PackageDef => p.stats.flatMap(extract _)
                case c: ClassDef => c :: c.impl.body.flatMap(extract _)
            }
        }

        Nil
    }
}
