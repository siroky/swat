package swat.common

import scala.collection.mutable
import scala.io.Source
import scala.concurrent._
import ExecutionContext.Implicits.global

/** Dependency on a type with the specified identifier. */
case class Dependency(identifier: String, isHard: Boolean)

/** A type source file with known dependencies. */
case class TypeSource(identifier: String, source: String, dependencies: List[Dependency])

/**
 * A loader of JavaScript files that depend on each other.
 */
@swat.ignored
object TypeLoader {

    /** Regex of a require statement in the source. */
    val Require = """swat\.require\('([^']+)', (true|false)\);""".r

    /**
     * Returns the JavaScript code of the specified types together with all their dependencies. The excludedTypes
     * aren't included in the result (they're considered to be already declared in the browser with all their
     * dependencies). Traverses the dependency graph and serializes it in the hard dependency order into one JavaScript
     * file.
     * @param typeIdentifiers Identifiers of the types that should be included in the result.
     * @param excludedTypes Identifiers of types that shouldn't be included in the result.
     */
    @swat.remote
    def get(typeIdentifiers: Array[String], excludedTypes: Array[String]): Future[String] = future {
        val sources = getNeededSources(typeIdentifiers.toList, excludedTypes.toSet)
        mergeSources(sources)
    }

    def getOrAlert(typeIdentifiers: Array[String], excludedTypes: Array[String]): Future[String] = {
        get(typeIdentifiers, excludedTypes).recover {
            case e: TypeLoadingException => s"alert('Swat type loading error: ${e.message}');"
        }
    }

    /**
     * Returns the JavaScript code of the specified object that extends the [[scala.App]] trait together with all
     * its dependencies. The application is started with the specified startup args provided.
     * @param appObjectTypeIdentifier Type identifier of the application object.
     * @param args The startup arguments.
     */
    @swat.remote
    def getApp(appObjectTypeIdentifier: String, args: Array[String]): Future[String] = {
        val typeIdentifier = appObjectTypeIdentifier.stripSuffix("$") + "$"
        val jsArgs = args.map("'" + _.replace("\\", "\\\\").replace("'", "\\'") + "'").mkString("[", ",", "]")

        get(Array[String](typeIdentifier), Array.empty).map { code =>
            s"""
               |$code
               |
               |// Application $appObjectTypeIdentifier start.
               |swat.startupArgs = swat.jsArrayToScalaArray($jsArgs);
               |$typeIdentifier();
            """.stripMargin
        }
    }

    def getAppOrAlert(appObjectTypeIdentifier: String, args: Array[String]): Future[String] = {
        getApp(appObjectTypeIdentifier, args).recover {
            case e: TypeLoadingException => s"alert('Swat application loading error: ${e.message}');"
        }
    }

    /**
     * Returns all dependencies from the specified source file.
     */
    def extractDependencies(source: String): List[Dependency] = {
        source.lines.collect { case Require(i, isHard) => Dependency(i, isHard == "true") }.toList
    }

    /**
     * Returns all sources together with their dependencies that have to be declared if the specified types should
     * work. It traverses a dependency graph of the specified types and returns all visited sources. The excluded
     * types are ignored during the traversal.
     */
    def getNeededSources(typeIdentifiers: List[String], excludedTypes: Set[String]): List[TypeSource] = {
        val sources = mutable.ListBuffer.empty[TypeSource]
        val toProcess = mutable.Set[String](typeIdentifiers: _*)
        val processed = mutable.Set.empty[String]

        def process(identifier: String) {
            toProcess -= identifier
            processed += identifier

            // Find all dependencies in the source and if they should be processed, enqueue them to the queue.
            val source = getSource(identifier)
            val dependencies = extractDependencies(source).flatMap { d =>
                val isExcluded = excludedTypes(d.identifier)
                if (!processed(d.identifier) && !isExcluded) {
                    toProcess += d.identifier
                }
                if (!isExcluded) Some(d) else None
            }
            sources += TypeSource(identifier, source, dependencies)
        }

        // While there is anything to process, process it.
        while (toProcess.nonEmpty) {
            process(toProcess.head)
        }
        sources.toList
    }

    /**
     * Returns content of the source file corresponding to the specified type.
     */
    def getSource(typeIdentifier: String): String = {
        val pathPrefix = typeIdentifier.replace(".", "/")
        val sourcePaths = List(".swat.js", ".js").map(pathPrefix + _)
        val classLoader = getClass.getClassLoader
        val sourceStream = sourcePaths.flatMap(p => Option(classLoader.getResourceAsStream(p))).headOption
        sourceStream.map(s => Source.fromInputStream(s).getLines().mkString("\n")).getOrElse {
            throw new TypeLoadingException(s"Cannot find source file of type $typeIdentifier.")
        }
    }

    /**
     * Merges the specified sources into one source by traversing the dependency graph in the hard dependency order.
     */
    def mergeSources(sources: List[TypeSource]): String = {
        val sourceGraph = sources.map(s => (s.identifier, s)).toMap
        val result = mutable.ListBuffer.empty[String]
        val toProcess = mutable.Set[String](sourceGraph.keys.toSeq: _*)
        val processing = mutable.ListBuffer.empty[String]
        val processed = mutable.Set.empty[String]

        def process(identifier: String) {
            if (processing.contains(identifier)) {
                val cycle = (processing :+ identifier).mkString(" -> ")
                throw new TypeLoadingException(s"Hard dependency cycle has been encountered ($cycle).")
            }
            if (!processed(identifier)) {
                // Move the type from toProcess to processing set.
                toProcess -= identifier
                processing += identifier

                // Process all hard dependencies of the type first and then add the type source to the result.
                val typeSource = sourceGraph(identifier)
                typeSource.dependencies.filter(_.isHard).foreach(d => process(d.identifier))
                result ++= typeSource.source.lines

                // Move the type from processing to processed set.
                processing -= identifier
                processed += identifier
            }
        }

        // While there is anything to process, process it.
        while (toProcess.nonEmpty) {
            process(toProcess.head)
        }
        result.mkString("\n")
    }
}

class TypeLoadingException(val message: String) extends Exception(message)
