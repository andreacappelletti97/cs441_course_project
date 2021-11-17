name := "log-kafka"

version := "0.1"

scalaVersion := "2.13.7"

val AkkaVersion = "2.6.15"
val JacksonVersion = "2.11.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-kafka" % "2.1.1",
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion,
  "io.spray" %%  "spray-json" % "1.3.6"
)