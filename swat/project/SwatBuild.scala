import sbt._
import Keys._

object SwatBuild extends Build
{
    val swatScalaVersion = "2.10.0-M7"

    val defaultSettings = Defaults.defaultSettings ++ Seq(
        scalaVersion := swatScalaVersion,
        scalacOptions ++= Seq(
            "-deprecation",
            "-unchecked",
            "-encoding", "utf8"
        ),
        libraryDependencies ++= Seq(
            "org.scalatest" % "scalatest_2.9.0" % "1.8" % "test"
        ),
        resolvers ++= Seq(
            DefaultMavenRepository
        ),
        version := "0.3"
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
        Project("api", file("api"), settings = defaultSettings)

    lazy val compilerProject =
        Project(
            "compiler", file("compiler"), settings = defaultSettings ++ Seq(
                libraryDependencies ++= Seq(
                    "org.scala-lang" % "scala-compiler" % swatScalaVersion
                )
            )
        ).dependsOn(
            apiProject
        )

    lazy val runtimeProject =
        Project(
            "runtime", file("runtime"), settings = defaultSettings
        ).dependsOn(
            compilerProject
        )
}
