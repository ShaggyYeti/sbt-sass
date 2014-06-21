package sass

import java.io.File
import com.typesafe.sbt.web.SbtWeb
import sbt.Keys._
import sbt._


object Import {
  val sass = TaskKey[Seq[File]]("sass", "Generate css files from scss and sass")
  val sassEntryPoints = SettingKey[PathFinder]("Finder for sass and scss files")
  val sassOptions = SettingKey[Seq[String]]("sassOptions", "Additional sass options")
}


object SbtSass extends AutoPlugin {
  override def requires = SbtWeb

  override def trigger = AllRequirements

  val autoImport = Import

  import SbtWeb.autoImport._
  import WebKeys._
  import autoImport._

  val baseSbtSassSettings = Seq(
    sassEntryPoints <<= (sourceDirectory in Compile)(base => ((base / "assets" ** "*.sass") +++ (base / "assets" ** "*.scss") --- base / "assets" ** "_*")),
    sassOptions := Seq.empty[String],
    moduleName in sass := "sass",

    sass := {
      val targetDir = webTarget.value / (moduleName in sass).value / "main"

//      val targetDir = webTarget.value / sourceFileTask.key.label / "main"

      def paths = sassEntryPoints.value.get
      paths.map(file => {
        val z = resourceManaged.value
        val x = targetDir
        val c = target.value
        val v = (public in Assets).value
        val b = streams.value.cacheDirectory
        println(resourceManaged.value)
        val fileName = (file.getName).replace(".sass", "").replace(".scss", "")
        val targetFileCss = targetDir / fileName.concat(".css")
        val targetFileCssMin = targetDir / fileName.concat(".min.css")

        val (css, cssMin) = SassCompiler.compile(file, sassOptions.value)

        IO.write(targetFileCss, css)
        IO.write(targetFileCssMin, cssMin)

        SbtWeb.copyResourceTo(
          (public in Assets).value / (moduleName in sass).value,
          targetFileCss.toURI().toURL(),
          streams.value.cacheDirectory / "copy-resource"
        )

        SbtWeb.copyResourceTo(
          (public in Assets).value / (moduleName in sass).value,
          targetFileCssMin.toURI().toURL(),
          streams.value.cacheDirectory / "copy-resource"
        )

        targetFileCss
      })
    },
    resourceGenerators <+= sass
  )

  override def projectSettings: Seq[Setting[_]] = inConfig(Assets)(baseSbtSassSettings)

}
