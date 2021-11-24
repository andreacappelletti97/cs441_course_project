package SampleSpark

import Cassandra.CassandraSetup
import Model.{CassLogModel, LogModel}
import Model.LogModel._
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import io.circe.parser._
import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector

import java.util.UUID
class SparkStreamKafka

object SparkStreamKafka extends java.io.Serializable{

  def main(array: Array[String]): Unit = {
    System.setProperty("hadoop.home.dir", "/")

    // Set up spark conf
    val conf = new SparkConf().setMaster("local[*]").setAppName("Spark-Kafka")
    // Add cass config to spark
    CassandraSetup(conf);

    // Create Spark context and Spark Stream
    val sparkContext = new SparkContext(conf)
    val streamContext = new StreamingContext(sparkContext, Seconds(2))

    // Create Table and KeySpace If Not Exists
    CassandraConnector(sparkContext).withSessionDo { session =>
      session.execute("CREATE KEYSPACE IF NOT EXISTS log_gen_keyspace WITH REPLICATION = { 'class': 'SimpleStrategy', 'replication_factor': '1' };".stripMargin)
      session.execute("CREATE TABLE IF NOT EXISTS log_gen_keyspace.log_data (log_id uuid PRIMARY KEY, timestamp text, log_type text, log_message text);".stripMargin)
    }

    // Create Kafka config
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "localhost:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "groupSpark",
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    // Choose Kafka Topic
    val topics = Array("Logs")
    val stream = KafkaUtils.createDirectStream[String, String](
      streamContext,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )

    // Parse and only get value in kafka stream
    val dStream: DStream[CassLogModel] = stream
      .map(record => record.value)
      .map(logJson => {
        val logEither = parse(logJson)
        logEither match {
          case Right(logModel) => logModel.as[LogModel] match {
            case Right(logModel) => CassLogModel(UUID.randomUUID(), logModel.message, logModel.level, logModel.timestamp)
            case Left(pf) => null
          }
          case Left(pf) => null
        }
      })

    // Listen To Stream
    dStream.foreachRDD(rdd => {
      if (rdd != null && !rdd.isEmpty()) {
        rdd.saveToCassandra("log_gen_keyspace", "log_data")
      }
    })

    // Spin up the spark stream
    streamContext.start()
    streamContext.awaitTermination()
  }
}
