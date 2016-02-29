lazy val playjsonsnake = project in file(".")

organization := "com.beamly"
name := "playjsonsnake"
licenses := Seq(("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0")))
description := "A micro-library that adds snake case support to play-json"
homepage := Some(url("https://github.com/beamly/playjsonsnake"))
startYear := Some(2016)

scalaVersion := "2.11.7"
crossScalaVersions := Seq("2.11.7", "2.10.6")

scalacOptions ++= Seq("-encoding", "utf8")
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint")
scalacOptions  += "-language:higherKinds"
scalacOptions  += "-language:implicitConversions"
scalacOptions  += "-language:postfixOps"
scalacOptions  += "-Xfuture"
scalacOptions  += "-Yno-adapted-args"
scalacOptions  += "-Ywarn-dead-code"
scalacOptions  += "-Ywarn-numeric-widen"
//scalacOptions ++= "-Ywarn-unused-import".ifScala211Plus.value.toSeq
scalacOptions  += "-Ywarn-value-discard"

maxErrors := 5
triggeredMessage := Watched.clearWhenTriggered

libraryDependencies += "com.typesafe.play" %% "play-json"   % "2.4.6"
libraryDependencies += "org.specs2"        %% "specs2-core" % "3.6.5" % "test"

parallelExecution in Test := true
fork in Test := false

pomExtra := pomExtra.value ++ {
  <developers>
    <developer>
      <id>emma-burrows</id>
      <name>Emma Burrows</name>
      <email>emma beamly com</email>
      <url>beamly.com</url>
    </developer>
  </developers>
    <scm>
      <connection>scm:git:github.com/beamly/playjsonsnake.git</connection>
      <developerConnection>scm:git:git@github.com:beamly/playjsonsnake.git</developerConnection>
      <url>https://github.com/beamly/playjsonsnake</url>
    </scm>
}

bintrayOrganization in ThisBuild := Some("beamly")
bintrayReleaseOnPublish in ThisBuild := false

releaseCrossBuild := true

watchSources ++= (baseDirectory.value * "*.sbt").get
watchSources ++= (baseDirectory.value / "project" * "*.scala").get
