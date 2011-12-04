name := "scaldi"

organization := "org.angelsmasterpiece.scaldi"

version := "1.0-SNAPSHOT"

crossScalaVersions := Seq("2.9.1")

scalaVersion := "2.9.1"

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "1.6.1" % "test"
)

pomExtra := <xml:group>
  <inceptionYear>2011</inceptionYear>
  <name>Scaldi - Scala Dependency Injection Framework</name>
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
    <url>http://github.com/OlegIlyenko/scaldi/issues</url>
  </issueManagement>
  <scm>
    <connection>scm:git://github.com/OlegIlyenko/scaldi.git</connection>
    <url>http://github.com/OlegIlyenko/scaldi/tree/master</url>
  </scm>
</xml:group>