import sbt._
import Keys._

object SwatBuild extends Build {

    val swatScalaVersion = "2.10.0"

    val defaultSettings = Defaults.defaultSettings ++ Seq(
        scalaVersion := swatScalaVersion,
        scalacOptions ++= Seq(
            "-deprecation",
            "-unchecked",
            "-feature",
            "-encoding", "utf8",
            "-language:implicitConversions"
        ),
        libraryDependencies ++= Seq(
            "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"
        ),
        resolvers ++= Seq(
            DefaultMavenRepository
        ),
        organization := "swat",
        version := "0.3-SNAPSHOT"
    )

    lazy val swatProject =
        Project(
            "swat", file("."), settings = defaultSettings
        ).aggregate(
            apiProject,
            compilerProject,
            runtimeProject
        )

    lazy val apiProject =
        Project("swat-api", file("api"), settings = defaultSettings)

    val swatTask = TaskKey[Unit]("swat")

    lazy val compilerProject =
        Project(
            "swat-compiler", file("compiler"), settings = defaultSettings ++ Seq(
                libraryDependencies ++= Seq(
                    "org.scala-lang" % "scala-compiler" % swatScalaVersion
                ),
                swatTask <<= (fullClasspath in Compile, runner, sourceDirectory in Compile, target in Compile) map { (cp, runner, src, target) =>
                    val logger = ConsoleLogger()
                    Run.executeTrapExit({
                        Run.run("swat.compiler.Main", cp.map(_.data), Seq("./runtime/src", "swat.runtime.client"), logger)(runner)
                    }, logger)
                }
            )
        ).dependsOn(
            apiProject
        )

    lazy val runtimeProject =
        Project(
            "swat-runtime", file("runtime"), settings = defaultSettings
        ).dependsOn(
            apiProject
        )

    /* Can't be currently used as the SBT requires its plugins to be built against the same version of scala.
    (compile in Compile) <<= (compile in Compile, managedClasspath in Compile, unmanagedClasspath in Compile, dependencyClasspath in Compile) map { (analysis, mcp, ucp, dcp) =>
        try {
            println("[info] Swat compilation started.")

            val separator = System.getProperty("path.separator")
            val cp = (mcp ++ ucp ++ dcp).map(_.data.getPath).distinct.mkString(separator)
            val compiler = new swat.compiler.SwatCompiler(cp, None, Some("./src/main/resources"))
            val sourceFiles = analysis.stamps.sources.keys.toList
            compiler.compile(sourceFiles)

            println("[info] Swat compilation successfully finished.")
        } catch {
            case t: Throwable => {
                println("[error] Swat compilation error.")
                println(t.getMessage)
                println(t.getStackTrace.mkString("\n"))
            }
        }

        analysis
    }*/
}
