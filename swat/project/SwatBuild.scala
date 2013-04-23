import sbt._
import Keys._
import play.Project._

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
                    "org.scala-lang" % "scala-compiler" % swatScalaVersion,
                    "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"
                )
            )
        ).dependsOn(
            apiProject
        )

    object SwatProject {
        def apply(id: String, path: java.io.File, settings: Seq[Setting[_]]): Project = {
            Project(
                id, path, settings = settings ++ Seq(
                    compile <<= (compile in Compile, fullClasspath in Compile, resourceDirectory in Compile, runner) map { (analysis, cp, res, runner) =>
                        val pathSeparator = System.getProperty("path.separator")
                        val swatCp = cp.map(_.data.getPath).mkString(pathSeparator)
                        val sourceFilePaths = analysis.infos.allInfos.keys.map(_.getPath).mkString(pathSeparator)

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
            ).dependsOn(compilerProject, apiProject)
        }
    }

    lazy val runtimeProject =
        Project(
            "swat-runtime", file("runtime"), settings = defaultSettings
        ).aggregate(
            internalRuntimeProject,
            commonRuntimeProject,
            clientRuntimeProject
        ).dependsOn(
            internalRuntimeProject,
            commonRuntimeProject,
            clientRuntimeProject
        )

    lazy val internalRuntimeProject =
        SwatProject("swat-internal", file("runtime/internal"), defaultSettings)

    lazy val commonRuntimeProject =
        SwatProject("swat-common", file("runtime/common"), defaultSettings)

    lazy val clientRuntimeProject =
        SwatProject(
            "swat-client", file("runtime/client"), defaultSettings
        ).dependsOn(
            commonRuntimeProject
        )

    lazy val webProject =
        play.Project(
            "swat-web", swatVersion, Nil, path = file("web")
        ).dependsOn(
            runtimeProject
        )
}
