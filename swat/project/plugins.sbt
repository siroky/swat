logLevel := Level.Warn

resolvers ++= Seq(
    DefaultMavenRepository,
    "SBT IDEA Repository" at "http://mpeltonen.github.com/maven/",
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.1.0")