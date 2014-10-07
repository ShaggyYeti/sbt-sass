package sass

//import play.PlayExceptions.AssetCompilationException

import java.io.File
import scala.sys.process._
import scala.Some
import scala.io.Source

object SassCompiler {
  def compile(sassFile: File, outfile: File, outfileMin: File, opts: Seq[String]): Seq[String] = {
    val options = opts
    try {
      val parentPath = sassFile.getParentFile.getAbsolutePath

      runCompiler(
        sassCommand ++ Seq("-l", "-I", parentPath) ++ options ++ Seq(Seq(sassFile.getAbsolutePath,  ":",   outfile.getAbsolutePath).mkString)
      )

      runCompiler(
        sassCommand ++ Seq("-t", "compressed", "-I", parentPath) ++ options ++ Seq(Seq(sassFile.getAbsolutePath,  ":",   outfileMin.getAbsolutePath).mkString)
      )

      // searching for imported dependencies
      getDependencies(outfile)

    } catch {
      case e: SassCompilationException => {
        throw new Exception("\nSass compiler: " + e.message +"\nFile: " + (e.file.orElse(Some(sassFile))).get.getCanonicalPath + "\nLine: " + e.line + " Col: " + Some(e.column).get)
//        throw AssetCompilationException(e.file.orElse(Some(sassFile)), "Sass compiler: " + e.message, Some(e.line), Some(e.column))
      }
    }
  }

  private def getSassPathInLinux: String = {

    val out = new StringBuilder

    val capturer = ProcessLogger(
      (output: String) => out.append(output),
      (error: String) => ()
    )

    def command = Seq("which", "sass") ! (capturer)
    if(command == 0) out.toString()
    else throw new Exception("'sass' command not found. Try to add path to 'sass' to your $PATH system variable")
  }

  private def getDependencies = { (cssFile: File) =>
    def result = Source.fromFile(cssFile).getLines().collect {
      case DependencyLine(f) => f
    }
    result.toList.distinct
  }

  private def sassCommand = if (isWindows) Seq("cmd","/c","sass.bat") else Seq(getSassPathInLinux)

  private val isWindows = System.getProperty("os.name").toLowerCase().indexOf("win") >= 0

  private val DependencyLine = """^/\* line \d+, (.*) \*/$""".r

  private def runCompiler(command: ProcessBuilder) = {
    val err = new StringBuilder
    val out = new StringBuilder

    val capturer = ProcessLogger(
      (output: String) => out.append(output + "\n"),
      (error: String) => err.append(error + "\n"))

    val process = command.run(capturer)
    if (process.exitValue != 0) {
      throw new SassCompilationException(err.toString)
    }
  }

  private val LocationLine = """\s*on line (\d+) of (.*)""".r

  private class SassCompilationException(stderr: String) extends RuntimeException {

    val (file: Option[File], line: Int, column: Int, message: String) = parseError(stderr)

    private def parseError(error: String): (Option[File], Int, Int, String) = {
      var line = 0
      var seen = 0
      var column = 0
      var file: Option[File] = None
      var message = "Unknown error, try running sass directly"
      for (errline: String <- augmentString(error).lines) {
        errline match {
          case LocationLine(l, f) => {
            line = l.toInt;
            file = Some(new File(f));
          }
          case other if (seen == 0) => {
            message = other;
            seen += 1
          }
          case other =>
        }
      }
      (file, line, column, message)
    }
  }

}
