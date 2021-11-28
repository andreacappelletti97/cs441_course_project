name := "Spark"

version := "0.1"

scalaVersion := "2.12.0"

val logbackVersion = "1.3.0-alpha10"
val sfl4sVersion = "2.0.0-alpha5"
val typesafeConfigVersion = "1.4.1"
val apacheCommonIOVersion = "2.11.0"
val scalacticVersion = "3.2.9"
val generexVersion = "1.0.2"
val sparkVersion = "3.2.0"
val awsSesSDK = "1.12.115"
val circe = "0.14.1"
val freeMarkerVersion = "2.3.14"

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "org.freemarker" % "freemarker" % freeMarkerVersion,
  "com.amazonaws" % "aws-java-sdk-ses" % awsSesSDK,
  "ch.qos.logback" % "logback-core" % logbackVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "org.slf4j" % "slf4j-api" % sfl4sVersion,
  "com.typesafe" % "config" % typesafeConfigVersion,
  "commons-io" % "commons-io" % apacheCommonIOVersion,
  "org.scalactic" %% "scalactic" % scalacticVersion,
  "org.scalatest" %% "scalatest" % scalacticVersion % Test,
  "org.scalatest" %% "scalatest-featurespec" % scalacticVersion % Test,
  "com.typesafe" % "config" % typesafeConfigVersion,
  "com.github.mifmif" % "generex" % generexVersion,
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion,
  "com.datastax.spark" %% "spark-cassandra-connector" % "3.1.0",
  "io.circe" %% "circe-core" % circe,
  "io.circe" %% "circe-generic" % circe,
  "io.circe" %% "circe-parser" % circe,

  "software.aws.mcs" % "aws-sigv4-auth-cassandra-java-driver-plugin" % "4.0.4",
  "com.datastax.oss" % "java-driver-core" % "4.13.0",
)
