package SampleSpark

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe


class SparkStreamKafka

object SparkStreamKafka {

  def main(array: Array[String]): Unit = {
    System.setProperty("hadoop.home.dir", "/")

    // Create spark conf and streaming context
    val conf = new SparkConf().setMaster("local[1]").setAppName("Spark-Kafka")
    val ssc = new StreamingContext(conf, Seconds(2))

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
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )

    val dStream: DStream[String] = stream.map(record => record.value)

    dStream.foreachRDD(rdd => {
      if (!rdd.isEmpty()) {
        println(s"Something Rdd Count: ${rdd.count()}")
        println(s"Example: ${rdd.top(10).mkString}")
      }
    })

    ssc.start()
    ssc.awaitTermination()
  }
}
