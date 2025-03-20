name := """Garuda"""
organization := "io.github.annabellegillet"

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala, PackPlugin)

scalaVersion := "2.13.10"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test

// Database
libraryDependencies ++= Seq(
	"com.typesafe.play" %% "play-slick" % "5.0.0",
	"com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
	"com.h2database" % "h2" % "1.4.200"
)

// https://mvnrepository.com/artifact/org.postgresql/postgresql
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.5"

// Apache commons io
libraryDependencies += "commons-io" % "commons-io" % "2.11.0"

// https://mvnrepository.com/artifact/net.liftweb/lift-json
libraryDependencies += "net.liftweb" %% "lift-json" % "3.5.0"

// Twitter API
libraryDependencies += "com.twitter" % "twitter-api-java-sdk" % "2.0.3" exclude("com.fasterxml.jackson.core", "jackson-databind")

// To resolve conflict
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.11.4"
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-annotations" % "2.11.4"
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson.core" % "2.11.4"

// Bluesky API
libraryDependencies ++= Seq(
	"com.atproto" % "atproto-java" % "0.1.0",
	"com.atproto" % "atproto-api" % "0.1.0"
)