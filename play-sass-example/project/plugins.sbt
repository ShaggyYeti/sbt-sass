resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.5")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.0.0")

lazy val root = Project("plugins", file(".")).dependsOn(plugin)

lazy val plugin = file("../").getCanonicalFile.toURI
