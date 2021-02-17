name := "scaldi"
organization := "org.scaldi"

description := "Scaldi - Scala Dependency Injection Library"
homepage := Some(url("https://github.com/scaldi/scaldi"))
licenses := Seq("Apache License, ASL Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

scalaVersion := "2.13.4"
crossScalaVersions := Seq("2.11.12", "2.12.10", "2.13.4")

mimaPreviousArtifacts := Set("0.6.0").map(organization.value %% name.value % _)

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.2",
  "com.typesafe" % "config" % "1.4.1" % Optional,
  "org.scalatest" %% "scalatest" % "3.2.3" % Test
)

fork := true
git.remoteRepo := "git@github.com:scaldi/scaldi.git"

// Publishing

pomIncludeRepository := (_ => false)
Test / publishArtifact := false
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowScalaVersions := crossScalaVersions.value
ThisBuild / githubWorkflowJavaVersions ++= Seq("adopt@1.11")
ThisBuild / githubWorkflowPublishTargetBranches :=  Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)

// Site and docs

enablePlugins(SiteScaladocPlugin)
enablePlugins(GhpagesPlugin)

// nice *magenta* prompt!

ThisBuild / shellPrompt := { state =>
  scala.Console.MAGENTA + Project.extract(state).currentRef.project + "> " + scala.Console.RESET
}

// Additional meta-info

startYear := Some(2011)
organizationHomepage := Some(url("https://github.com/scaldi"))
scmInfo := Some(ScmInfo(
  browseUrl = url("https://github.com/scaldi/scaldi"),
  connection = "scm:git:git@github.com:scaldi/scaldi.git"
))
developers := List(
  Developer("AprilAtProtenus", "April Hyacinth", "april@protenus.com", url("https://github.com/AprilAtProtenus")),
  Developer("dave-handy", "Dave Handy", "wdhandy@gmail.com", url("https://github.com/dave-handy"))
)
