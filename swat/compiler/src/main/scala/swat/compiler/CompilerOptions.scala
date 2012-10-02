package swat.compiler

import java.io.File

case class CompilerOptions(target: Option[File])
{
    def toList = {
        target.map(t =>  "%s:%s".format(CompilerOptions.targetOption, t.getAbsolutePath)).toList
    }
}

object CompilerOptions
{
    val targetOption = "target"

    def help(pluginName: String): String = {
        "  -P:%s:%s:dir             Sets the target directory for JavaScript files to dir.".format(
            pluginName,
            targetOption)
    }

    def apply(options: List[String]): CompilerOptions = {
        val optionMap = options.flatMap { o =>
            o.split(":").toList match {
                case option :: value :: _ => Some(option, value)
                case option :: _ => Some(option, "")
                case _ => None
            }
        }.toMap

        CompilerOptions(optionMap.get(targetOption).map(new File(_)))
    }

    def default = CompilerOptions(None)
}
