package swat.compiler

/**
 * A console application that invokes the Swat compiler. Takes three command line arguments:
 *   - Compilation class path.
 *   - Paths of the source files.
 *   - Target directory for the JavaScript files.
 */
object Main extends App {
    val List(cp, sourceFilePaths, javaScriptTarget) = args.toList
    val sourceFiles = sourceFilePaths.split(System.getProperty("path.separator")).map(new java.io.File(_)).toList
    val compiler = new SwatCompiler(Some(cp), None, Some(javaScriptTarget))

    println(s"[swat] Compiling ${sourceFiles.length} Scala sources into JavaScript to $javaScriptTarget...")
    try {
        val result = compiler.compile(sourceFiles)
        result.warnings.foreach(w => println(s"[warning] $w"))
        result.infos.foreach(i => println(s"[info] $i"))
    } catch {
        case c: CompilationException => println(c.message)
    }
}
