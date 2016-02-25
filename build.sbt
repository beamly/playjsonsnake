lazy val playjsonsnake = project in file(".")

organization := "com.beamly"
name := "playjsonsnake"
licenses := Seq(("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0")))
description := "Play-Json snake case to camel case field conversions"
homepage := Some(url("https://github.com/beamly/playjsonsnake"))

scalaVersion := "2.11.7"
crossScalaVersions := Seq("2.11.7", "2.10.6")

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-encoding", "utf8")

maxErrors := 5
triggeredMessage := Watched.clearWhenTriggered

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json"   % "2.4.6" exclude("org.scala-lang", "scala-reflect"),
  "org.specs2"        %% "specs2-core" % "3.6.5" % Test)

parallelExecution in Test := true
fork in Test := false

bintrayOrganization := Some("beamly")

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

bintrayReleaseOnPublish in ThisBuild := false

releaseCrossBuild := true

watchSources ++= (baseDirectory.value * "*.sbt").get
watchSources ++= (baseDirectory.value / "project" * "*.scala").get
