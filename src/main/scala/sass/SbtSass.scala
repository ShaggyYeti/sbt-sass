package sass

import com.typesafe.sbt.web.{incremental, SbtWeb}
import sbt.Keys._
import sbt._
import com.typesafe.sbt.web.incremental._
import com.typesafe.sbt.web.incremental.OpSuccess

object Import {
  val sass = TaskKey[Seq[File]]("sass", "Generate css files from scss and sass")
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
    excludeFilter in sass := HiddenFileFilter || "_*",
    includeFilter in sass := "*.sass" || "*.scss",

    managedResourceDirectories += (resourceManaged in sass in Assets).value,
    resourceManaged in sass in Assets := webTarget.value / "sass" / "main",
    resourceGenerators in Assets <+= sass in Assets,

    sass in Assets := Def.task {
      val sourceDir = (sourceDirectory in Assets).value
      val targetDir = (resourceManaged in sass in Assets).value
      val sources = (sourceDir ** ((includeFilter in sass in Assets).value -- (excludeFilter in sass in Assets).value)).get

      val results = incremental.syncIncremental((streams in Assets).value.cacheDirectory / "run", sources) {
        modifiedSources: Seq[File] =>
          if (modifiedSources.size > 0) 
            streams.value.log.info(s"Sass compiling on ${modifiedSources.size} source(s)")
            
          val compilationResults = modifiedSources map { source => {
            val sourceName = source.getPath.drop(sourceDir.getPath.size).reverse.dropWhile(_ != '.').reverse
            def sourceWithExtn(extn: String): File = targetDir / (sourceName + extn)
            val targetFileCss = sourceWithExtn("css")
            val targetFileCssMap = sourceWithExtn("css.map")
            val targetFileCssMin = sourceWithExtn("min.css")
            val targetFileCssMinMap = sourceWithExtn("min.css.map")

            // prepares folders for results of sass compiler
            targetFileCss.getParentFile.mkdirs()

            // sass compiles and creates files
            val dependencies = SassCompiler.compile(source, targetFileCss, targetFileCssMin, sassOptions.value)

            val readFiles = (dependencies.map (file)).toSet + source
            ((targetFileCss,
              targetFileCssMin,
              targetFileCssMap,
              targetFileCssMinMap),
              source,
              OpSuccess(readFiles, Set(
                targetFileCss,
                targetFileCssMin,
                targetFileCssMap,
                targetFileCssMinMap)))
            }
          }
          val createdFiles = (compilationResults map (_._1)).foldLeft(Seq.empty[File]) { (createdFilesList, targetFiles) => 
            createdFilesList ++ Seq(targetFiles._1, targetFiles._2)
          }
          val cachedForIncrementalCompilation = compilationResults.foldLeft(Map.empty[File, OpResult]) { (acc, sourceAndResultFiles) => 
            acc ++ Map((sourceAndResultFiles._2, sourceAndResultFiles._3))
          }
          (cachedForIncrementalCompilation, createdFiles)
      }
      println(s"${results._2.toSet}")
      (results._1 ++ results._2.toSet).toSeq
    }.dependsOn(WebKeys.webModules in Assets).value,
    
    sassOptions in Assets := (webModuleDirectories in Assets).value.getPaths.foldLeft(Seq.empty[String]){ (acc, str) => acc ++ Seq("-I", str) }
  )

  override def projectSettings: Seq[Setting[_]] = inConfig(Assets)(baseSbtSassSettings)
}
