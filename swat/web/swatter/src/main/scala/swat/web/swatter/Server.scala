package swat.web.swatter

import swat.remote
import scala.concurrent._
import ExecutionContext.Implicits.global
import swat.compiler.SwatCompiler
import swat.common.{TypeSource, TypeLoader}

/**
 * A package of compiled code.
 * @param compiledCode Just the code that was compiled. Not runnable as is.
 * @param runnableCode The compiled code together with all its dependencies, so it's runnable.
 */
case class CodePackage(compiledCode: String, runnableCode: String)

/**
 * A remote object whose methods are invokable directly from the [[swat.web.swatter.Client]].
 */
@remote object Server {

    /**
     * Compiles the specified Scala code to JavaScript. The result is returned as a code package so it's possible both
     * to display just the compiled code and to have the compiled code with all its dependencies which can be executed.
     */
    def compile(scalaCode: String): Future[CodePackage] = future {
        // Compile the code and turn the type outputs to type sources.
        val compiler = new SwatCompiler(None, None, None)
        val compilationOutput = compiler.compile(scalaCode)
        val typeSources = compilationOutput.typeOutputs.map { o =>
            TypeSource(o.identifier, o.code, TypeLoader.extractDependencies(o.code))
        }

        // Create the code package.
        val compiledCode = typeSources.map(_.source).mkString("\n\n")
        val compiledTypes = typeSources.map(_.identifier)
        val dependencies = typeSources.flatMap(_.dependencies.map(_.identifier))
        val neededSources = TypeLoader.getNeededSources(dependencies, compiledTypes.toSet)
        val runnableCode = TypeLoader.mergeSources(typeSources ++ neededSources)
        CodePackage(compiledCode, runnableCode)
    }
}
