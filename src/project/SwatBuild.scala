import sbt._
import Keys._
import sbt.inc.Analysis
import scala.collection._
import scala.tools.nsc.io.Directory
import com.typesafe.sbt.SbtStartScript

object SwatBuild extends Build {

    val swatScalaVersion = "2.10.2"
    val swatVersion = "0.4-SNAPSHOT"

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

    val swatTask = TaskKey[Analysis]("swat", "Swat compilation")

    object RawSwatProject {

        /* For each project its last compilation time or nothing if it hasn't been compiled yet. */
        val projectCompilationTimes = mutable.HashMap[String, Long]()

        def apply(id: String, path: java.io.File, settings: Seq[Setting[_]] = defaultSettings): Project = {
            Project(
                id, path, settings = settings ++ Seq(
                    swatTask <<= (compile in Compile, fullClasspath in Compile, resourceDirectory in Compile, runner) map {
                        (analysis, cp, res, runner) =>

                        // Discover the modified sources.
                        val lastCompilationTime = projectCompilationTimes.getOrElse(id, 0L)
                        val sourceCompilationTimes = analysis.apis.internal.mapValues(_.compilation.startTime)
                        val changedSources = sourceCompilationTimes.filter(_._2 > lastCompilationTime).map(_._1)
                        projectCompilationTimes.update(id, sourceCompilationTimes.values.max)

                        // If anything has been modified, recompile it.
                        if (changedSources.nonEmpty) {
                            val pathSeparator = System.getProperty("path.separator")
                            val swatClassPath = cp.map(_.data.getPath).mkString(pathSeparator)
                            val sourcePaths = changedSources.map(_.getAbsolutePath).mkString(pathSeparator)

                            // Run the compiler.
                            val logger = ConsoleLogger()
                            Run.executeTrapExit(Run.run(
                                "swat.compiler.Main", // Class to run.
                                cp.map(_.data), // Classpath for the compiler.
                                Seq(swatClassPath, sourcePaths, res.getPath), // Command line arguments.
                                logger
                            )(runner), logger)
                        }

                        analysis
                    },
                    compile <<= swatTask
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
            "web", swatVersion, Nil, path = file("web"), defaultSettings ++ SbtStartScript.startScriptForClassesSettings
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
