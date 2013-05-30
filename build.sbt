import com.jsuereth.sbtsite.SiteKeys

name := "scaldi"

organization := "com.github.scaldi"

version := "0.1.3-SNAPSHOT"

crossScalaVersions := Seq("2.9.2", "2.10.1")

scalaVersion := "2.10.1"

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "1.9.1" % "test"
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

seq(site.settings: _*)

seq(ghpages.settings: _*)

git.remoteRepo := "git@github.com:scaldi/scaldi.git"

site.addMappingsToSiteDir(mappings in packageDoc in Compile, "api")

SiteKeys.siteMappings <<=
  (SiteKeys.siteMappings, PamfletKeys.write, PamfletKeys.output) map { (mappings, _, dir) =>
    mappings ++ (dir ** "*.*" x relativeTo(dir))
  }

pomExtra := <xml:group>
  <url>http://scaldi.github.io/scaldi/Scaldi.html</url>
  <inceptionYear>2011</inceptionYear>
  <name>Scaldi - Scala Dependency Injection Library</name>
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