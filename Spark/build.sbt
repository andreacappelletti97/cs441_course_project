name := "Spark"

version := "0.1"

scalaVersion := "2.11.0"

val logbackVersion = "1.2.3"
val sfl4sVersion = "1.7.16"
val typesafeConfigVersion = "1.4.1"
val apacheCommonIOVersion = "2.11.0"
val scalacticVersion = "3.2.9"
val generexVersion = "1.0.2"
val sparkVersion = "2.4.7"
val awsSesSDK = "1.11.516"
val circe = "0.11.2"
val freeMarkerVersion = "2.3.31"
val datastax = "4.2.2"

resolvers += Resolver.jcenterRepo

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) =>
    (xs map {_.toLowerCase}) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) => MergeStrategy.discard
      case _ => MergeStrategy.last
    }
  case x => MergeStrategy.first
}


libraryDependencies ++= Seq(
  "org.freemarker" % "freemarker" % freeMarkerVersion,
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

  "io.circe" %% "circe-core" % circe,
  "io.circe" %% "circe-generic" % circe,
  "io.circe" %% "circe-parser" % circe,

  "com.amazonaws" % "aws-java-sdk-ses" % awsSesSDK,

  "com.datastax.oss" % "java-driver-core" % datastax,
  "com.datastax.oss" % "java-driver-query-builder" % datastax,
  "com.datastax.oss" % "java-driver-mapper-runtime" % datastax,
)

assembly / assemblyJarName := "SparkConsumer.jar"
