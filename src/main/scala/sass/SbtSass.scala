package sass

import com.typesafe.sbt.web.{incremental, SbtWeb}
import sbt.Keys._
import sbt._
import com.typesafe.sbt.web.incremental._
import com.typesafe.sbt.web.incremental.OpSuccess

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
    // include webjar folders
    sassOptions := (webModuleDirectories in Assets).value.getPaths.foldLeft(Seq.empty[String]){ (acc, str) => acc ++ Seq("-I", str)},
    resourceManaged in sass in Assets := (webTarget in Assets).value / "sass" / "main",
    managedResourceDirectories += (resourceManaged in sass).value,

    sass := Def.task {
      // (file, relative_path)
      def files = sassEntryPoints.value pair relativeTo((sourceDirectory in Assets).value)
      // (file -> relative_path
      def fileMapToPath = files.foldLeft(Map.empty[java.io.File, String]) { (m, item) => m ++ Map(item)}
      val results = incremental.syncIncremental((streams in Assets).value.cacheDirectory / "run", sassEntryPoints.value.get) {
        modifiedFiles: Seq[File] =>
          if(modifiedFiles.size > 0) {streams.value.log.info(s"Sass compiling on ${modifiedFiles.size} source(s)")}
          val compilationResults = modifiedFiles.map {
            file => {
              val fileName = fileMapToPath(file).replace(".sass", "").replace(".scss", "")
              val targetFileCss = (resourceManaged in sass).value / fileName.concat(".css")
              val targetFileCssSourcemap = (resourceManaged in sass).value / fileName.concat(".css.map")
              val targetFileCssMin = (resourceManaged in sass).value / fileName.concat(".min.css")
              val targetFileCssMinSourcemap = (resourceManaged in sass).value / fileName.concat(".min.css.map")

              // prepares folders for results of sass compiler
              targetFileCss.getParentFile().mkdirs()

              // sass compiles and creates files
              val dependencies = SassCompiler.compile(file, targetFileCss, targetFileCssMin, sassOptions.value)

              val readFiles: Set[File] = (dependencies.map { new File(_) }).toSet + file
              ((targetFileCss,
                targetFileCssMin,
                targetFileCssSourcemap,
                targetFileCssMinSourcemap),
                file,
                OpSuccess(readFiles, Set(
                  targetFileCss,
                  targetFileCssMin,
                  targetFileCssSourcemap,
                  targetFileCssMinSourcemap)))
            }
          }
          val createdFiles = (compilationResults.map {_._1})
            .foldLeft(Seq.empty[File]) { (createdFilesList, targetFiles) => createdFilesList ++ Seq(targetFiles._1, targetFiles._2)}
          val cachedForIncrementalCompilation = compilationResults.foldLeft(Map.empty[File, OpResult]) { (acc, sourceAndResultFiles) => acc ++ Map((sourceAndResultFiles._2, sourceAndResultFiles._3))}
          (cachedForIncrementalCompilation, createdFiles)
      }
      (results._1 ++ results._2.toSet).toSeq
    }.dependsOn(WebKeys.webJars in Assets).value,
    resourceGenerators <+= sass
  )

  override def projectSettings: Seq[Setting[_]] = inConfig(Assets)(baseSbtSassSettings)
}
