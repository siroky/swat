logLevel := Level.Warn

scalaVersion := "2.10.0"

resolvers ++= Seq(
    "SBT IDEA Repository" at "http://mpeltonen.github.com/maven/",
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

// Doesn't currently support 2.10.0 version of scala.
// addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.2.0")

libraryDependencies ++= Seq(
    "swat" %% "swat-compiler" % "0.3-SNAPSHOT"
)
