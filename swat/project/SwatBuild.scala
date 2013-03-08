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
            "org.scalatest" % "scalatest_2.10.0" % "1.8" % "test"
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

    lazy val compilerProject =
        Project(
            "swat-compiler", file("compiler"), settings = defaultSettings ++ Seq(
                libraryDependencies ++= Seq(
                    "org.scala-lang" % "scala-compiler" % swatScalaVersion
                )
            )
        ).dependsOn(
            apiProject
        )

    lazy val runtimeProject =
        Project(
            "swat-runtime", file("runtime"), settings = defaultSettings ++ Seq(
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
                }
            )
        ).dependsOn(
            apiProject
        )
}
