package swat.compiler

import frontend.JsAstGenerator
import scala.tools.nsc.Global
import tools.nsc.plugins.{PluginComponent, Plugin}
import reflect.internal.Phase

class SwatCompilerPlugin(val global: Global) extends Plugin
{
    import global._

    val name = "swat-compiler"

    val description = "Swat Compiler of Scala code into JavaScript."

    val components = List[PluginComponent](SwatCompilationComponent)

    private var options = SwatCompilerOptions.default

    private var outputJsAst = js.Program.empty

    override val optionsHelp = Some(SwatCompilerOptions.help(name))

    override def processOptions(o: List[String], error: String => Unit) {
        super.processOptions(o, error)
        options = SwatCompilerOptions(o)
    }

    /**
     * Output JavaScript AST of the compiled code. An empty [[swat.compiler.js.Program]] if the compilation hasn't
     * finished yet or if an error occurred.
     */
    def output = outputJsAst

    /**
     * The main component of the Swat compilation. Transforms the Scala ASTs into equivalent ASTs and sets the result
     * into the output variable. If the target is specified, creates a file with JavaScript code generated from the
     * JavaScript ASTs in a directories corresponding to the packages.
     */
    private object SwatCompilationComponent
        extends PluginComponent
        with JsAstGenerator
    {
        val global: SwatCompilerPlugin.this.global.type = SwatCompilerPlugin.this.global

        val runsAfter = List("uncurry")

        val phaseName = "swat-compilation"

        def newPhase(prev: Phase) = new StdPhase(prev) {
            def apply(unit: CompilationUnit) {
                outputJsAst = processCompilationUnit(unit)
            }
        }
    }
}
