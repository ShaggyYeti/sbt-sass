package sass

import java.io.File
import com.typesafe.sbt.web.SbtWeb
import sbt.Keys._
import sbt._


object Import {
  val sass = TaskKey[Seq[File]]("sass", "Generate css files from scss and sass")
  val sassEntryPoints = SettingKey[PathFinder]("Finder for sass and scss files")
  val sassOptions = SettingKey[Seq[String]]("sassOptions", "Additional sass options")
  val sassPublicDir = SettingKey[String]("sassPublicDir", "Directory name for storing published css files. Default: sass")
}


object SbtSass extends AutoPlugin {
  override def requires = SbtWeb

  override def trigger = AllRequirements

  val autoImport = Import

  import SbtWeb.autoImport._
  import WebKeys._
  import autoImport._

  val baseSbtSassSettings = Seq(
    sassEntryPoints <<= (sourceDirectory in Compile)(srcPath => ((srcPath / "public" ** "*.sass") +++ (srcPath / "public" ** "*.scss") --- srcPath / "public" ** "_*")),
    sassOptions := Seq.empty[String],
    moduleName in sass := "sass",
    sassPublicDir := (moduleName in sass).value,

    sass := {
      val targetDir = webTarget.value / (moduleName in sass).value / "main"
      def paths = sassEntryPoints.value.get
      paths.map(file => {
        val fileName = (file.getName).replace(".sass", "").replace(".scss", "")
        val targetFileCss = targetDir / fileName.concat(".css")
        val targetFileCssMin = targetDir / fileName.concat(".min.css")

        val (css, cssMin) = SassCompiler.compile(file, sassOptions.value)

        IO.write(targetFileCss, css)
        IO.write(targetFileCssMin, cssMin)

        val f = SbtWeb.copyResourceTo(
          (public in Assets).value / sassPublicDir.value,
          targetFileCss.toURI().toURL(),
          streams.value.cacheDirectory / "copy-resource"
        )

        SbtWeb.copyResourceTo(
          (public in Assets).value / sassPublicDir.value,
          targetFileCssMin.toURI().toURL(),
          streams.value.cacheDirectory / "copy-resource"
        )
        (public in Assets).value / sassPublicDir.value

        targetFileCss
      })
      Seq(targetDir)
    },
    resourceGenerators <+= sass
  )


  override def projectSettings: Seq[Setting[_]] = inConfig(Assets)(baseSbtSassSettings)

}
