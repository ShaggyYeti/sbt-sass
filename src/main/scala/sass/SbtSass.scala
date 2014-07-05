package sass

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
    sassEntryPoints <<= (sourceDirectory in Assets)(srcPath => ((srcPath ** "*.sass") +++ (srcPath ** "*.scss") --- srcPath ** "_*")),
    sassOptions := Seq.empty[String],
    resourceManaged in sass in Assets := (webTarget in Assets).value / "sass" / "main",
    managedResourceDirectories += (resourceManaged in sass).value,

    sass := {
      def paths = sassEntryPoints.value pair relativeTo((sourceDirectory in Assets).value)
      val cssFiles = paths.map {
        case (file, path) => {
          val fileName = path.replace(".sass", "").replace(".scss", "")
          val targetFileCss = (resourceManaged in sass).value / fileName.concat(".css")
          val targetFileCssMin = (resourceManaged in sass).value / fileName.concat(".min.css")

          val (css, cssMin) = SassCompiler.compile(file, sassOptions.value)

          IO.write(targetFileCss, css)
          IO.write(targetFileCssMin, cssMin)

          (targetFileCss, targetFileCssMin)
        }
      }
      seqOfTuple2Seq(cssFiles)
    },
    resourceGenerators <+= sass
  )

  /* converts Seq((a,b), (c,d), (e,f)) to Seq(a, b, c, d, e, f) */
  private def seqOfTuple2Seq[T](list: Seq[Tuple2[T, T]]): Seq[T] = {
    if (list != Nil) {
      def current = list.head
      Seq(current._1, current._2) ++ seqOfTuple2Seq(list.tail)
    } else {
      Nil
    }
  }

  override def projectSettings: Seq[Setting[_]] = inConfig(Assets)(baseSbtSassSettings)
}
