logLevel := Level.Warn

resolvers ++= Seq(
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
)

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.1")

addSbtPlugin("play" % "sbt-plugin" % "2.1.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-start-script" % "0.8.0")
