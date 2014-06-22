name := "scaldi"

description := "Scaldi - Scala Dependency Injection Library"

organization := "org.scaldi"

version := "0.4.1-SNAPSHOT"

scalaVersion := "2.11.1"

scalacOptions += "-deprecation"

fork := true

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scalatest" %% "scalatest" % "2.1.3" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.1" % "test"
)

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { x => false }

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

// nice *magenta* prompt!
shellPrompt in ThisBuild := { state =>
  scala.Console.MAGENTA + Project.extract(state).currentRef.project + "> " + scala.Console.RESET
}

git.remoteRepo := "git@github.com:scaldi/scaldi.git"

site.settings

site.includeScaladoc()

ghpages.settings

pomExtra := <xml:group>
  <url>http://scaldi.org</url>
  <inceptionYear>2011</inceptionYear>
  <licenses>
    <license>
      <name>Apache License, ASL Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <developers>
    <developer>
      <id>OlegIlyenko</id>
      <name>Oleg Ilyenko</name>
    </developer>
  </developers>
  <issueManagement>
    <system>GitHub</system>
    <url>http://github.com/scaldi/scaldi/issues</url>
  </issueManagement>
  <scm>
    <connection>scm:git:git@github.com:scaldi/scaldi.git</connection>
    <url>git@github.com:scaldi/scaldi.git</url>
  </scm>
</xml:group>
