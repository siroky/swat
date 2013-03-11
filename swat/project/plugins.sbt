logLevel := Level.Warn

resolvers ++= Seq(
    "SBT IDEA Repository" at "http://mpeltonen.github.com/maven/",
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.2.0")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.2")
