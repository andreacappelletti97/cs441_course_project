import Model.{CassLogModel, KafkaLogModel}
import Util.{CreateLogger, ObtainConfigReference}
import com.datastax.oss.driver.api.core.{ConsistencyLevel, CqlSession}
import com.datastax.oss.driver.api.mapper.annotations.Transient
import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.toRDDFunctions
import io.circe.parser.parse
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

import java.util.UUID

class SparkConsumer

object SparkConsumer extends java.io.Serializable {
  private val logger = CreateLogger(classOf[SparkConsumer])
  private val config = ObtainConfigReference("spark") match {
    case Some(value) => value
    case None => throw new RuntimeException("Can't obtain reference to the config")
  }
  @Transient @transient private val session = CqlSession.builder.build()
  private val cassLocation = config.getString("spark.cassandra.location")


  /**
   * Create A Spark Consumer That will
   * listen to the latest data from kafka
   * and pushed it into cassandra and
   * run an aggregate algorithm upon it
   * @param None
   * @return None
   *
   */
  def main(args: Array[String]): Unit = {
    System.setProperty("hadoop.home.dir", "/")

    val conf = new SparkConf().setMaster("local[*]").setAppName("Spark-Kafka")
    conf.set("spark.cassandra.connection.host", config.getString("spark.cassandra.host"))
    conf.set("spark.cassandra.connection.port", config.getString("spark.cassandra.port"))
    logger.info("Create Spark Config")

    val sparkContext = new SparkContext(conf)
    val streamContext = new StreamingContext(sparkContext, Seconds(2))
    logger.info("Create Spark Stream")

    if (cassLocation == "local") {
      CassandraConnector(sparkContext).withSessionDo { session =>
        session.execute("CREATE KEYSPACE IF NOT EXISTS log_gen_keyspace WITH REPLICATION = { 'class': 'SimpleStrategy', 'replication_factor': '1' };".stripMargin)
        session.execute("CREATE TABLE IF NOT EXISTS log_gen_keyspace.log_data (log_id uuid PRIMARY KEY, timestamp text, log_type text, log_message text);".stripMargin)
      }
    }

    val kafkaParams = kafkaConfig()
    val stream = createSparkStream(streamContext, kafkaParams)
    logger.info("Use Spark-Stream to listen to Kafka")

    val dstream: DStream[CassLogModel] = convertStreamToLogModel(stream)
    logger.info("Convert Kafka output to CassLogModel")

    // Listen To Stream Asynchronously
    dstream.foreachRDD(rdd => {
      if (rdd != null && !rdd.isEmpty()) {
        saveToCassandra(rdd)
      }
    })

    // Listen To Kafka Stream
    streamContext.start()
    streamContext.awaitTermination()
  }

  /**
   * Save RDD To Cassandra
   * @param rdd
   */
  def saveToCassandra(rdd: RDD[CassLogModel]): Unit = {
    cassLocation match {
      case "local" => saveToLocal(rdd)
      case "aws" => saveToCassAws(rdd)
      case _ => throw new RuntimeException("Cassandra location not supported")
    }
  }

  /**
   * Save To Local cassandra using spark-cass-connector
   * @param rdd
   */
  def saveToLocal(rdd: RDD[CassLogModel]): Unit = {
    rdd.saveToCassandra(config.getString("spark.cassandra.keyspace"), config.getString("spark.cassandra.table"))
  }

  /**
   * Loop Through RDD and save each CasslogModel into aws keyspace
   * @param rdd
   */
  def saveToCassAws(rdd: RDD[CassLogModel]): Unit = {
    rdd.foreach(cassLogModel => {
      if (cassLogModel != null) {
        save(cassLogModel)
      }
    })
  }

  /**
   * Save cassLogModel into aws keyspace using cql
   * @param cassLogModel
   */
  def save(cassLogModel: CassLogModel): Unit = {
    @Transient @transient val prepareStatement = session.prepare(
      """INSERT INTO log_gen_keyspace.log_data
        |   (log_id, log_message, log_type, timestamp) VALUES
        |   (?, ?, ?, ?)""".stripMargin)
    @Transient @transient val parameterizedStatement = prepareStatement.bind(cassLogModel.log_id, cassLogModel.log_message, cassLogModel.log_type, cassLogModel.timestamp)
    @Transient @transient val statement = parameterizedStatement.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
    session.execute(statement)
  }

  /**
   * Convert (key, value) from kafka into CassLogModel
   * using circe json parser
   * @param stream
   * @return DStream
   */
  def convertStreamToLogModel(stream: InputDStream[ConsumerRecord[String, String]]): DStream[CassLogModel] = {
    stream
      .map(record => record.value())
      .map(logJson => {
        val logEither = parse(logJson)
        logEither match {
          case Right(logModel) => logModel.as[KafkaLogModel] match {
            case Right(logModel) => CassLogModel(UUID.randomUUID(), logModel.message, logModel.level, logModel.timestamp)
            case Left(_) => null
          }
          case Left(_) => null
        }
      })
  }

  /**
   * Simple Create Stream that directly connecto to kafka
   * And listen to the topic
   * @param streamContext
   * @param kafkaParams
   * @return
   */
  def createSparkStream(streamContext: StreamingContext, kafkaParams: Map[String, Object]): InputDStream[ConsumerRecord[String, String]] = {
    val topics = Array(config.getString("spark.kafka.topic"))
    val stream = KafkaUtils.createDirectStream[String, String](
      streamContext,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )
    stream
  }

  /**
   * Kafka configuration
   * @return
   */
  def kafkaConfig(): Map[String, Object] = {
    Map[String, Object](
      "bootstrap.servers" -> s"${config.getString("spark.host")}:${config.getLong("spark.port")}",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> config.getString("spark.groupId"),
      "auto.offset.reset" -> config.getString("spark.offset"),
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )
  }

}
