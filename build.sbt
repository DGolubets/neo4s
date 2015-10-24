name := "neo4s"

organization := "ru.dgolubets"

version := "0.1.1-SNAPSHOT"

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-feature", "-deprecation")

libraryDependencies ++= Seq(
  // main
  "com.typesafe.play" %% "play-iteratees" % "2.4.3",
  "com.typesafe.play.extras" %% "iteratees-extras" % "1.5.0",
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0",
  "com.typesafe.akka" %% "akka-http-core-experimental" % "1.0",
  "com.typesafe.play" %% "play-json" % "2.4.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",

  // test
  "ch.qos.logback" % "logback-classic" % "1.1.1"  % "test",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.14" % "test",
  "com.typesafe.akka" %% "akka-stream-testkit-experimental" % "1.0" % "test"
)

// publish

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

licenses := Seq("Apache License" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("https://github.com/DGolubets/neo4s"))

pomExtra :=
  <scm>
    <url>git@github.com:DGolubets/neo4s.git</url>
    <connection>scm:git:git@github.com:DGolubets/neo4s.git</connection>
  </scm>
    <developers>
      <developer>
        <id>DGolubets</id>
        <name>Dmitry Golubets</name>
        <email>dgolubets@gmail.com</email>
      </developer>
    </developers>