name := "dipendi"
organization := "com.protenus"
version := "0.6.0-SNAPSHOT"

description := "Dipendi - Scala Dependency Injection Library"
homepage := Some(url("https://github.com/protenus/dipendi"))
licenses := Seq("Apache License, ASL Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

crossScalaVersions := Seq("2.11.12", "2.12.10", "2.13.1")
scalaVersion := "2.13.1"

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.3",
  "com.typesafe" % "config" % "1.4.0" % Optional,
  "org.scalatest" %% "scalatest" % "3.1.0" % Test
)

fork := true
git.remoteRepo := "git@github.com:protenus/dipendi.git"

// Publishing

publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := (_ => false)
publishTo := Some(
  if (version.value.trim.endsWith("SNAPSHOT"))
    "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  else
    "releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")

// Site and docs

enablePlugins(SiteScaladocPlugin)
enablePlugins(GhpagesPlugin)

// nice *magenta* prompt!

shellPrompt in ThisBuild := { state =>
  scala.Console.MAGENTA + Project.extract(state).currentRef.project + "> " + scala.Console.RESET
}

// Additional meta-info

startYear := Some(2011)
organizationHomepage := Some(url("https://github.com/protenus"))
scmInfo := Some(ScmInfo(
  browseUrl = url("https://github.com/protenus/dipendi"),
  connection = "scm:git:git@github.com:protenus/dipendi.git"
))
pomExtra := <xml:group>
  <developers>
    <developer>
      <id>protenus</id>
      <name>Protenus</name>
    </developer>
  </developers>
</xml:group>