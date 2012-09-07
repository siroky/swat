package swat.compiler

import java.io.File

case class SwatCompilerOptions(target: File, mirrorPackages: Boolean)
{
    def toList = List(
        "%s:%s".format(SwatCompilerOptions.targetOption, target.getAbsolutePath),
        "%s:%s".format(SwatCompilerOptions.mirrorPackagesOption, mirrorPackages.toString)
    )
}

object SwatCompilerOptions
{
    val targetOption = "target"

    val mirrorPackagesOption = "mp"

    def help(pluginName: String) =
        """|  -P:%1$s:%s    The target directory for JavaScript files.
           |  -P:%1$s:%s    Whether a directory structure mirroring the packages is created.""".stripMargin.format(
            pluginName, targetOption, mirrorPackagesOption
        )

    def apply(options: List[String]): SwatCompilerOptions = {
        val optionMap = options.flatMap { o =>
            o.split(":").toList match {
                case option :: value :: _ => Some(option, value)
                case option :: _ => Some(option, "")
                case _ => None
            }
        }.toMap

        SwatCompilerOptions(
            optionMap.get(targetOption).map(new File(_)).getOrElse(default.target),
            optionMap.get(mirrorPackagesOption).map(_.toBoolean).getOrElse(default.mirrorPackages)
        )
    }

    def default = SwatCompilerOptions(new File("."), true)
}
