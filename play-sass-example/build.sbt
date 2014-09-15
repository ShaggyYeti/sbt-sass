name := """play-sass-example"""

version := "1.0-SNAPSHOT"

//sassOptions in Assets ++= Seq("--compass", "-r", "compass")

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.3.0",
  "org.webjars" % "foundation" % "5.3.0",
  javaJdbc,
  javaEbean,
  cache,
  javaWs
)
