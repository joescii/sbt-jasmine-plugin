package com.joescii

import sbt._
import Keys._
import org.mozilla.javascript.{ContextFactory, Function => JsFunction}
import org.mozilla.javascript.tools.shell.Global
import java.io.{BufferedReader, InputStreamReader}


object SbtJasminePlugin extends Plugin {

  lazy val jasmineEdition = SettingKey[Int]("jasmineEdition", "The edition of Jasmine to use, i.e. the major version number 1 or 2")
  lazy val jasmineTestDir = SettingKey[Seq[File]]("jasmineTestDir", "Path to directory containing the /specs and /mocks directories")
  lazy val appJsDir = SettingKey[Seq[File]]("appJsDir", "the root directory where the application js files live")
  lazy val appJsLibDir = SettingKey[Seq[File]]("appJsLibDir", "the root directory where the application's js library files live")
  lazy val jasmineConfFile = SettingKey[Seq[File]]("jasmineConfFile", "the js file that loads your js context and configures jasmine")
  lazy val jasmineRequireJsFile = SettingKey[Seq[File]]("jasmineRequireJsFile", "the require.js file used by the application")
  lazy val jasmineRequireConfFile = SettingKey[Seq[File]]("jasmineRequireConfFile", "the js file that configures require to find your dependencies")
  lazy val jasmine = TaskKey[Unit]("jasmine", "Run jasmine tests")

  lazy val jasmineOutputDir = SettingKey[File]("jasmineOutputDir", "directory to output jasmine files to.")
  lazy val jasmineGenRunner = TaskKey[Unit]("jasmine-gen-runner", "Generates a jasmine test runner html page.")

  def validateEdition(edition:Int) = if(edition != 1 && edition != 2) throw new RuntimeException("jasmineEdition must be 1 or 2")

  private val VersionRegex = """^\Qversion=\E(.*)$""".r
  lazy val cl = this.getClass.getClassLoader
  type WebJarInfo = Option[(String, String)] // (Version, Root)

  def webJarVersion(pkg:String):Option[String] = for {
    pomProps <- Option(cl.getResourceAsStream("META-INF/maven/"+pkg+"/jasmine/pom.properties"))
    propReader = new BufferedReader(new InputStreamReader(pomProps))
    version <- Stream.continually(propReader.readLine()).takeWhile(_ != null).collectFirst { case VersionRegex(v) => v }
  } yield {
    pomProps.close()
    version
  }

  lazy val classicWebJar:WebJarInfo = for {
    version <- webJarVersion("org.webjars")
    jasmine <- Option(cl.getResource("META-INF/resources/webjars/jasmine/"+version+"/jasmine.js"))
  } yield { (version, "META-INF/resources/webjars/jasmine/"+version) }

  lazy val bowerWebJar:WebJarInfo = for {
    version <- webJarVersion("org.webjars.bower")
    jasmine <- Option(cl.getResource("META-INF/resources/webjars/jasmine/"+version+"/lib/jasmine-core/jasmine.js"))
  } yield { (version, "META-INF/resources/webjars/jasmine/"+version+"/lib/jasmine-core") }

  lazy val webJar:WebJarInfo = Stream(classicWebJar, bowerWebJar).collectFirst { case Some(pair) => pair }

  lazy val webjarJasmineVersion:Option[String] = webJar.map(_._1)

  lazy val webjarJasmineEdition:Option[Int] = webjarJasmineVersion flatMap { v =>
    try { Some(v.take(1).toInt) } catch { case _:Exception => None }
  }

  /** First looks to see if there is a jasmine webjar on the path. If not found, then use what we deliver */
  def jasmineResourceRoot(edition:Int):String = webJar.map(_._2).getOrElse("jasmine"+edition)

  def jasmineTask = (jasmineTestDir, appJsDir, appJsLibDir, jasmineConfFile, jasmineOutputDir, jasmineEdition, streams) map { 
    (testJsRoots, appJsRoots, appJsLibRoots, confs, outDir, edition, s) =>

    validateEdition(edition)

    val version = webjarJasmineVersion.getOrElse(
      if(edition == 1) "1.3.1"
      else "2.0.3"
    )
    val resourceRoot = jasmineResourceRoot(edition)

    s.log.info("running jasmine "+version+"...")

    val errorCounts = for {
        testRoot <- testJsRoots
        appJsRoot <- appJsRoots
        appJsLibRoot <- appJsLibRoots
        conf <- confs
    } yield {
      val jscontext =  new ContextFactory().enterContext()
      val scope = new Global()
      scope.init(jscontext)

      jscontext.evaluateString(scope, "var arguments = [];", "Evil Hack to simulate command-line args to help r.js", 0, null)
      jscontext.evaluateString(scope, "var jasmineEdition = "+edition+";", "jasmineEdition.js", 0, null)
      jscontext.evaluateString(scope, "var jasmineResourceRoot = \""+resourceRoot+"\";", "Pointing to the root of jasmine resources", 0, null)
      jscontext.evaluateReader(scope, bundledScript("sbtjasmine.js"), "sbtjasmine.js", 1, null)

      val jasmineEnvHtml = outDir / "jasmineEnv.html"
      outputBundledResource("jasmineEnv.html", jasmineEnvHtml)

      // js func = function runTests(appJsRoot, appJsLibRoot, testRoot, confFile)
      val runTestFunc = scope.get("runTests", scope).asInstanceOf[JsFunction]
      val errorsInfile = runTestFunc.call(jscontext, scope, scope, Array(
        appJsRoot.getAbsolutePath,
        appJsLibRoot.getAbsolutePath,
        testRoot.getAbsolutePath,
        conf.getAbsolutePath,
        jasmineEnvHtml.getAbsolutePath))

      errorsInfile.asInstanceOf[Double]
    }
    
    val errorCount = errorCounts.sum
    if (errorCount > 0) throw new TestsFailedException()
  }

  def jasmineGenRunnerTask = (jasmineOutputDir, jasmineTestDir, appJsDir, appJsLibDir, jasmineRequireJsFile, jasmineRequireConfFile, jasmineEdition, streams) map { 
    (outDir, testJsRoots, appJsRoots, appJsLibRoots, requireJss, requireConfs, edition, s) =>

    validateEdition(edition)

    s.log.info("generating runner...")

    val dir = "jasmine"+edition

    outputBundledResource(dir+"/jasmine.js", outDir / "jasmine.js")
    if(edition == 2) {
      outputBundledResource(dir+"/boot.js", outDir / "boot.js")
      outputBundledResource(dir+"/boot-rhino.js", outDir / "boot-rhino.js")
    }
    outputBundledResource(dir+"/jasmine-html.js", outDir / "jasmine-html.js")
    outputBundledResource(dir+"/jasmine.css", outDir / "jasmine.css")

    val isWin = java.lang.System.getProperty("os.name").indexOf("Windows") > -1

    for {
      testRoot <- testJsRoots
      appJsRoot <- appJsRoots
      appJsLibRoot <- appJsLibRoots
      requireJs <- requireJss
      requireConf <- requireConfs
    } {
      val runnerString =
        if(isWin) {
          loadRunnerTemplate.format(
            "file:///" + testRoot.getAbsolutePath.replaceAll("\\\\", "\\\\\\\\"),
            "file:///" + appJsRoot.getAbsolutePath.replaceAll("\\\\", "\\\\\\\\"),
            "file:///" + appJsLibRoot.getAbsolutePath.replaceAll("\\\\", "\\\\\\\\"),
            "file:///" + requireJs.getAbsolutePath.replaceAll("\\\\", "\\\\\\\\"),
            "file:///" + requireConf.getAbsolutePath.replaceAll("\\\\", "\\\\\\\\"),
            generateSpecRequires(testRoot),
            getRunnerFn(edition)
          )
        } else {
          loadRunnerTemplate.format(
            testRoot.getAbsolutePath,
            appJsRoot.getAbsolutePath,
            appJsLibRoot.getAbsolutePath,
            requireJs.getAbsolutePath,
            requireConf.getAbsolutePath,
            generateSpecRequires(testRoot),
            getRunnerFn(edition)
          )
        }
      IO.write(outDir / "runner.html", runnerString)
    }
    s.log.info("output to: file://" + (if(isWin) "/" else "") + (outDir / "runner.html" getAbsolutePath) )

  }

  def loadRunnerTemplate = {
    val cl = this.getClass.getClassLoader
    val is = cl.getResourceAsStream("runnerTemplate.html")
    val template = scala.io.Source.fromInputStream(is).getLines().mkString("\n")

    is.close
    template
  }

  def generateSpecIncludes(testRoot: File) = {
    val specsDir = testRoot / "specs" ** "*spec.js"

    specsDir.get.map("""<script type="text/javascript" src="""" + _.getAbsolutePath + """"></script>""").mkString("\n")
  }

  def generateSpecRequires(testRoot: File) = {
    val specFiles = testRoot / "specs" ** "*spec.js"

    val specModules = specFiles.get.map { path =>
      testRoot.toURI().relativize(path.toURI()).getPath
    }.map(_.replaceFirst(".js$", ""))

    specModules.map("'" + _ + "'").mkString(", ")
  }

  def getRunnerFn(edition:Int) =
    if(edition == 1)
      """
        |jasmine.getEnv().addReporter(new jasmine.TrivialReporter());
        |jasmine.getEnv().execute();
      """.stripMargin
    else
      """
        |// need to check if onload handler set by boot.js has been executed
        |if(window.alreadyLoaded === true){
        |    // if onload handler has been executed we can safely invoke jasmine
        |    jasmine.getEnv().execute();
        |}else{
        |    // if onload handler has not been executed we need to modify it to invoke jasmine too
        |    var currentWindowOnload = window.onload;
        |    window.onload = function() {
        |        if (currentWindowOnload) {
        |            currentWindowOnload();
        |        }
        |        jasmine.getEnv().execute();
        |    };
        |}
      """.stripMargin

  def bundledScript(fileName: String) = {
    val cl = this.getClass.getClassLoader
    val is = cl.getResourceAsStream(fileName)

    new InputStreamReader(is)
  }

  def outputBundledResource(resourcePath: String, outputPath: File) {
    try {
      val cl = this.getClass.getClassLoader
      val is = cl.getResourceAsStream(resourcePath)

      IO.transfer(is, outputPath)
      is.close()
    }

  }

  val jasmineSettings: Seq[Project.Setting[_]] = Seq(
    jasmine <<= jasmineTask,
    appJsDir := Seq(),
    appJsLibDir := Seq(),
    jasmineTestDir := Seq(),
    jasmineConfFile := Seq(),
    jasmineRequireJsFile := Seq(),
    jasmineRequireConfFile := Seq(),
    jasmineGenRunner <<= jasmineGenRunnerTask,
    jasmineOutputDir <<= (target in test) { d => d / "jasmine"},
    jasmineEdition := webjarJasmineEdition.getOrElse(2)
  )
}

// Used in JS, DO NOT REMOVE
class BundledLibraryReaderFactory(resourcePath: String) {
  lazy val cl = this.getClass.getClassLoader
  def reader = new InputStreamReader(cl.getResourceAsStream(resourcePath))
}
