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
        conflictWarning := ConflictWarning.disable,
        organization := "swat",
        version := swatVersion,
        libraryDependencies ++= Seq(
            "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"
        )
    )

    lazy val swat =
        Project(
            "swat", file("."), settings = defaultSettings
        ).aggregate(
            api,
            compiler,
            runtime,
            web
        )

    lazy val api =
        Project("api", file("api"), settings = defaultSettings)

    lazy val compiler =
        Project(
            "compiler", file("compiler"), settings = defaultSettings ++ Seq(
                libraryDependencies ++= Seq(
                    "org.scala-lang" % "scala-compiler" % swatScalaVersion
                )
            )
        ).dependsOn(
            api
        )

    val swatTask = TaskKey[Unit]("swat", "Swat compilation")

    object RawSwatProject {
        def apply(id: String, path: java.io.File, settings: Seq[Setting[_]] = defaultSettings): Project = {
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
                compiler, api
            )
        }
    }

    object SwatProject {
        def apply(id: String, path: java.io.File, settings: Seq[Setting[_]] = defaultSettings) = {
            RawSwatProject(id, path, settings).dependsOn(
                runtime
            )
        }
    }

    lazy val runtime =
        Project(
            "runtime", file("runtime"), settings = defaultSettings
        ).aggregate(
            runtimeJava,
            runtimeScala,
            runtimeCommon,
            runtimeClient
        ).dependsOn(
            runtimeJava,
            runtimeScala,
            runtimeCommon,
            runtimeClient
        )

    lazy val runtimeJava =
        RawSwatProject("java", file("runtime/java"))

    lazy val runtimeScala =
        RawSwatProject("scala", file("runtime/scala"), defaultSettings ++ Seq(autoScalaLibrary := false))

    lazy val runtimeCommon =
        RawSwatProject(
            "common", file("runtime/common"), defaultSettings ++ Seq(
                libraryDependencies += "play" %% "play" % "2.1.1"
            )
        )

    lazy val runtimeClient =
        RawSwatProject(
            "client", file("runtime/client")
        ).dependsOn(
            runtimeCommon
        )

    lazy val web =
        play.Project(
            "web", swatVersion, Nil, path = file("web")
        ).aggregate(
            tests,
            swatter,
            playground
        ).dependsOn(
            tests,
            swatter,
            playground
        )

    lazy val tests =
        SwatProject("tests", file("web/tests"))

    lazy val swatter =
        SwatProject("swatter", file("web/swatter"))

    lazy val playground =
        SwatProject("playground", file("web/playground"))
}
