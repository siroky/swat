package swat.compiler

import java.io.File

case class SwatCompilerOptions(target: Option[File])
{
    def toList = {
        List(
            target.map(t =>  "%s:%s".format(SwatCompilerOptions.targetOption, t.getAbsolutePath))
        ).flatten
    }
}

object SwatCompilerOptions
{
    val targetOption = "target"

    def help(pluginName: String): String = {
        "  -P:%s:%s:dir             Sets the target directory for JavaScript files to dir.".format(
            pluginName,
            targetOption
        )
    }

    def apply(options: List[String]): SwatCompilerOptions = {
        val optionMap = options.flatMap { o =>
            o.split(":").toList match {
                case option :: value :: _ => Some(option, value)
                case option :: _ => Some(option, "")
                case _ => None
            }
        }.toMap

        SwatCompilerOptions(optionMap.get(targetOption).map(new File(_)))
    }

    def default = SwatCompilerOptions(None)
}
