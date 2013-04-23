package swat.compiler

object Main extends App {
    val List(cp, sourceFilePaths, javaScriptTarget) = args.toList
    val sourceFiles = sourceFilePaths.split(System.getProperty("path.separator")).map(new java.io.File(_)).toList
    val compiler = new SwatCompiler(Some(cp), None, Some(javaScriptTarget))

    println(s"[info] Swat compilation started ...")
    try {
        val result = compiler.compile(sourceFiles)
        result.warnings.foreach(w => println(s"[warning] $w"))
        result.infos.foreach(i => println(s"[info] $i"))
        println(s"[success] Swat compilation successfully finished.")
    } catch {
        case c: CompilationException => println(c.getMessage)
    }
}
