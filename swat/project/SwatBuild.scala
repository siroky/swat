import sbt._
import Keys._
import scala.tools.nsc.io.Directory

object SwatBuild extends Build {

    val swatScalaVersion = "2.10.1"
    val swatVersion = "0.3-SNAPSHOT"

    val defaultSettings = Defaults.defaultSettings ++ Seq(
        scalaVersion := swatScalaVersion,
        scalacOptions ++= Seq(
            "-deprecation",
            "-unchecked",
            "-feature",
            "-encoding", "utf8",
            "-language:implicitConversions"
        ),
        organization := "swat",
        version := swatVersion
    )

    lazy val swatProject =
        Project(
            "swat", file("."), settings = defaultSettings
        ).aggregate(
            apiProject, compilerProject, runtimeProject
        )

    lazy val apiProject =
        Project("api", file("api"), settings = defaultSettings)

    lazy val compilerProject =
        Project(
            "compiler", file("compiler"), settings = defaultSettings ++ Seq(
                libraryDependencies ++= Seq(
                    "org.scala-lang" % "scala-compiler" % swatScalaVersion,
                    "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"
                )
            )
        ).dependsOn(
            apiProject
        )

    val swatTask = TaskKey[Unit]("swat", "Swat compilation")

    object SwatProject {
        def apply(id: String, path: java.io.File, settings: Seq[Setting[_]]): Project = {
            Project(
                id, path, settings = settings ++ Seq(
                    swatTask <<= (fullClasspath in Compile, sourceDirectory in Compile, resourceDirectory in Compile, runner) map { (cp, src, res, runner) =>
                        val pathSeparator = System.getProperty("path.separator")
                        val swatCp = cp.map(_.data.getPath).mkString(pathSeparator)
                        val sourceFiles = new Directory(src / "scala").deepFiles
                        // val sourceFiles = analysis.infos.allInfos.keys
                        val sourceFilePaths = sourceFiles.map(_.path).mkString(pathSeparator)

                        // Run the compiler.
                        val logger = ConsoleLogger()
                        Run.executeTrapExit(Run.run(
                            "swat.compiler.Main", // Class to run.
                            cp.map(_.data), // Classpath for the compiler.
                            Seq(swatCp, sourceFilePaths, res.getPath), // Command line arguments.
                            logger
                        )(runner), logger)

                        analysis
                    }
                )
            ).dependsOn(
                compilerProject, apiProject
            )
        }
    }

    lazy val runtimeProject =
        Project(
            "runtime", file("runtime"), settings = defaultSettings
        ).aggregate(
            runtimeJavaProject,
            runtimeScalaProject,
            runtimeCommonProject,
            runtimeClientProject,
            runtimeTestsProject
        ).dependsOn(
            runtimeJavaProject,
            runtimeScalaProject,
            runtimeCommonProject,
            runtimeClientProject,
            runtimeTestsProject
        )

    lazy val runtimeJavaProject =
        SwatProject("java", file("runtime/java"), defaultSettings)

    lazy val runtimeScalaProject =
        SwatProject("scala", file("runtime/scala"), defaultSettings ++ Seq(autoScalaLibrary := false))

    lazy val runtimeCommonProject =
        SwatProject("common", file("runtime/common"), defaultSettings)

    lazy val runtimeClientProject =
        SwatProject(
            "client", file("runtime/client"), defaultSettings
        ).dependsOn(
            runtimeCommonProject
        )

    lazy val runtimeTestsProject =
        SwatProject(
            "tests", file("runtime/tests"), defaultSettings
        ).dependsOn(
            runtimeCommonProject, runtimeClientProject
        )

    lazy val webProject =
        play.Project(
            "web", swatVersion, Nil, path = file("web")
        ).dependsOn(
            runtimeProject
        )
}
