package swat.runtime.server

import scala.io.Source
import scala.collection.{immutable, mutable}

/**
 * A loader of JavaScript files that depend on each other.
 */
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
    def get(typeIdentifiers: List[String], excludedTypes: Set[String] = Set.empty): String = {
        val neededSources = getNeededSources(typeIdentifiers, excludedTypes)
        val sourceGraph = neededSources.map(s => (s.identifier, s)).toMap
        val result = mutable.ListBuffer.empty[String]
        val toProcess = mutable.Set[String](sourceGraph.keys.toSeq: _*)
        val processing = mutable.ListBuffer.empty[String]
        val processed = mutable.Set.empty[String]

        def process(identifier: String) {
            if (processing.contains(identifier)) {
                val cycle = (processing :+ identifier).mkString(" -> ")
                throw TypeLoadingException(s"Hard dependency cycle has been encountered ($cycle).")
            }
            if (!processed(identifier)) {
                // Move the type from toProcess to processing set.
                toProcess -= identifier
                processing += identifier

                // Process all dependencies of the type first and then add the type source to the result.
                val typeSource = sourceGraph(identifier)
                typeSource.dependencies.foreach(process _)
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

    /** A type source file with known dependencies. */
    case class TypeSource(identifier: String, source: String, dependencies: immutable.List[String])

    /**
     * Returns all sources together with their dependencies that have to be declared if the specified types should
     * work. It traverses a dependency graph of the specified types and returns all visited sources. The excluded
     * types are ignored during the traversal.
     */
    private def getNeededSources(typeIdentifiers: List[String], excludedTypes: Set[String]): List[TypeSource] = {
        val sources = mutable.ListBuffer.empty[TypeSource]
        val toProcess = mutable.Set[String](typeIdentifiers: _*)
        val processed = mutable.Set.empty[String]

        def process(identifier: String) {
            toProcess -= identifier
            processed += identifier

            // Find all dependencies in the source and if they should be processed, enqueue them to the queue.
            val source = getSource(identifier)
            val dependencies = source.lines.collect { case Require(i, isHard) =>
                val isExcluded = excludedTypes(i)
                if (!processed(i) && !isExcluded) {
                    toProcess += i
                }
                if (isHard == "true" && !isExcluded) Some(i) else None
            }
            sources += TypeSource(identifier, source, dependencies.flatten.toList)
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
    private def getSource(typeIdentifier: String): String = {
        val sourcePath = typeIdentifier.replace(".", "/") + ".js"
        val sourceStream = Option(getClass.getClassLoader.getResourceAsStream(sourcePath))
        sourceStream.map(s => Source.fromInputStream(s).getLines().mkString("\n")).getOrElse {
            throw TypeLoadingException(s"Can't find source file of type '$typeIdentifier'.")
        }
    }
}

case class TypeLoadingException(message: String) extends Exception(message)
