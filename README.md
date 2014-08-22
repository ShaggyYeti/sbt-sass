sbt-sass for Play Framework 2.3.x
========
Plugin based on [play-sass][play-sass] 

## ver 0.1.6 [23 aug 2014]
* Added support of sourcemaps generation, thanks to [@onelson][onelson]
* `--sourcemaps` parameter has hardcoded . It is absent in `sass` versions before `3.3`.  

## ver 0.1.5 [02 aug 2014]
* Fixes #9 : added WebJars integration

# Prerequisites
[Sass][sass] compiler needs to be installed for plugin to work. This means that `sass` executable
needs to be found in path. Sass can be installed by installing `sass` gem (**minimal version 3.3**).
```
gem install sass -v 3.4.0 
```
You can verify that `sass` has been installed by following command:
```
sass -v
```
Also you should install (optional) compass if you want to use it
```
gem install compass
```

# Installation
## External GitHub repository (Recommended)
1. Add line to `project/plugins.sbt`
   ```
   resolvers += Resolver.url("GitHub repository", url("http://shaggyyeti.github.io/releases"))(Resolver.ivyStylePatterns)

   addSbtPlugin("default" % "sbt-sass" % "0.1.6")
   ```
2. Run `activator`

## Compilation from sources
1. Clone from this repo
   ```
   git clone https://github.com/ShaggyYeti/sbt-sass.git
   ```
2. Enter to folder with sbt-sass and run `sbt`
3. In sbt-console execute command `publishLocal`
4. Add line in your play project to project/plugins.sbt
   ```
   addSbtPlugin("default" % "sbt-sass" % "0.1.6")
   ```
5. Run `activator`

# Usage
* `*.sass` and `*.scss` files in `app/assets` directories will be automatically compiled to `*.css` files
* Files starting with `_`-character will be left out from compilation as per Play convention.
* For example the result of compilation of `app/assets/test.scss` you can include by next line:
```
<link rel="stylesheet" media="screen" href="@routes.Assets.at("test.css")">
```
* also, you can use `sbt-sass` with `WebJars`. For example, webjar of Foundation:
  * add next line to `build.sbt`
   ```
   libraryDependencies ++= Seq(
     "org.webjars" %% "webjars-play" % "2.3.0",
     "org.webjars" % "foundation" % "5.3.0"
    )
   ```
  * then you can include in scss-file:
   ```
   @import "lib/foundation/scss/foundation";
   ```


Example of play-project in `play-sass-example` folder

## Compass (optional)
If u want to use [compass][compass] just add next line in `build.sbt`:
```
sassOptions in Assets ++= Seq("--compass", "-r", "compass")
```

## Troubles and solutions
* Cannot run program "sass": error=2, No such file or directory
  * *In linux:* you should add path to sass command to environment variable $PATH
  * *Running from IntelliJ IDEA:* your IDE doesn't read environment variables, you should set PATH to sass in IDE settings. Solutions and documentation: [issue #6][issue6], [Idea Help: path variables][idea-env-vars]


[play-sass]: https://github.com/jlitola/play-sass
[sass]: http://sass-lang.com/
[compass]: http://compass-style.org/
[play-2.3-anatomy]: http://www.playframework.com/documentation/2.3.x/Anatomy
[issue6]: https://github.com/ShaggyYeti/sbt-sass/issues/6#issuecomment-49294238
[idea-env-vars]: http://www.jetbrains.com/idea/webhelp/path-variables-2.html
[onelson]: https://github.com/onelson