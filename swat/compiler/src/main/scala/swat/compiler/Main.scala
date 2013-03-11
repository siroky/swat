package swat.compiler

import scala.reflect.io.Directory

object Main extends App {
    val srcPath = args.head
    val packages = args.tail.headOption
    val scalaSrcPath = srcPath + "/main/scala"
    val resourcesPath = srcPath + "/main/resources"
    val packageDirs = packages.map(_.replace(".", "/").split(";").map(scalaSrcPath + "/" + _).toList)
    val sourceDirNames = packageDirs.getOrElse(List(scalaSrcPath))
    val sourceDirs = sourceDirNames.map(n => new Directory(new java.io.File(n)))
    val sourceFiles = sourceDirs.flatMap(_.deepFiles.toList.map(_.jfile))
    val compiler = new SwatCompiler(None, None, Some(resourcesPath))

    println(s"[info] Swat compiling all sources in ${sourceDirs.mkString(", ")} ...")
    try {
        val result = compiler.compile(sourceFiles)
        result.warnings.foreach(w => println(s"[warning] $w"))
        result.infos.foreach(i => println(s"[info] $i"))
        println(s"[success] Swat compilation successfully finished.")
    } catch {
        case c: CompilationException => println(c.getMessage)
    }
}
