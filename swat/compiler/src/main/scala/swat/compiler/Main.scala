package swat.compiler

import scala.reflect.io.Directory

object Main extends App {
    val srcPath = args.head
    val scalaSrcPath = srcPath + "/main/scala"
    val resourcesPath = srcPath + "/main/resources"
    val sourceFiles = new Directory(new java.io.File(scalaSrcPath)).deepFiles.toList.map(_.jfile)
    val compiler = new SwatCompiler(None, None, Some(resourcesPath))

    println(s"[info] Swat compiling all sources in $scalaSrcPath ...")
    try {
        val result = compiler.compile(sourceFiles)
        result.warnings.foreach(w => println(s"[warning] $w"))
        result.infos.foreach(i => println(s"[info] $i"))
        println(s"[success] Swat compilation successfully finished.")
    } catch {
        case c: CompilationException => println(c.getMessage)
    }
}
