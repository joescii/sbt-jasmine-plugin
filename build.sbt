sbtPlugin := true

name := "sbt-jasmine-plugin"

organization := "com.joescii"

version := "1.2-SNAPSHOT"

libraryDependencies += "org.mozilla" % "rhino" % "1.7R4"

// don't bother publishing javadoc
publishArtifact in (Compile, packageDoc) := false

scalaVersion := "2.9.2"

scalacOptions ++= Seq("-unchecked", "-deprecation")

publishTo <<= (version) { version: String =>
    val publishType = if (version.endsWith("SNAPSHOT")) "snapshots" else "releases"
    Some(
        Resolver.file(
            "guardian github " + publishType,
            file(System.getProperty("user.home") + "/guardian.github.com/maven/repo-" + publishType)
        )
    )
}

