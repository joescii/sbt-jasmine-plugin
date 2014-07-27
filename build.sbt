import bintray.Keys._

sbtPlugin := true

name := "sbt-jasmine-plugin"

organization := "com.joescii"

version := "1.2.2-SNAPSHOT"

libraryDependencies += "org.mozilla" % "rhino" % "1.7R4"

// don't bother publishing javadoc
publishArtifact in (Compile, packageDoc) := false

sbtVersion in Global := {
  scalaBinaryVersion.value match {
    case "2.10" => "0.13.5"
    case "2.9.2" => "0.12.4"
  }
}

scalaVersion in Global := "2.9.2"

crossScalaVersions := Seq("2.9.2", "2.10.4")

scalacOptions ++= Seq("-unchecked", "-deprecation")

publishMavenStyle := false

bintrayPublishSettings

repository in bintray := "sbt-plugins"

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

bintrayOrganization in bintray := None

seq(lsSettings :_*)

(LsKeys.tags in LsKeys.lsync) := Seq("sbt", "jasmine", "javascript", "testing")

(description in LsKeys.lsync) := "An sbt plugin for running jasmine tests in your build."

(LsKeys.ghUser in LsKeys.lsync) := Some("joescii")

(LsKeys.ghRepo in LsKeys.lsync) := Some("sbt-jasmine-plugin")

(LsKeys.ghBranch in LsKeys.lsync) := Some("master")