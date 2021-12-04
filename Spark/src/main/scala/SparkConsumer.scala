import AmazonSes.EmailService
import Model.CassLogModel
import Util.Utility.{kafkaConfig, parseToLogModel}
import Util.{CreateLogger, ObtainConfigReference}
import com.datastax.oss.driver.api.core.{ConsistencyLevel, CqlSession}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

class SparkConsumer

object SparkConsumer extends java.io.Serializable {
  private val logger = CreateLogger(classOf[SparkConsumer])
  private val config = ObtainConfigReference("spark") match {
    case Some(value) => value
    case None => throw new RuntimeException("Can't obtain reference to the config")
  }
  @transient private val session = CqlSession.builder.build()
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
    val conf = new SparkConf().setAppName("Spark-Kafka-Consumer")
    logger.info("Create Spark Config")

    val sparkContext = new SparkContext(conf)
    val streamContext = new StreamingContext(sparkContext, Seconds(2))
    logger.info("Create Spark Stream")

    val kafkaParams = kafkaConfig()
    val stream = createSparkStream(streamContext, kafkaParams)
    logger.info("Use Spark-Stream to listen to Kafka")

    val dstream = convertStreamToLogModel(stream)
    logger.info("Convert Kafka output to CassLogModel")

    // Listen To Stream Asynchronously
    dstream.foreachRDD(rdd => {
      if (rdd != null && !rdd.isEmpty()) {
        // Save To Cass
        saveToCassandra(rdd)

        // Send Email
        sendErrorLogEmail(rdd)
      }
    })

    // Listen To Kafka Stream
    streamContext.start()
    streamContext.awaitTermination()
  }

  /**
   * Send Email with error list using AWS SES
   * @param errorList
   */
  def sendErrorLogEmail(rdd: RDD[CassLogModel]): Unit = {
    val errorList = filterError(rdd);
    if (errorList.size == 3) {
      logger.info(s"Sending Email: ${errorList}")
      EmailService.populateTemplate(errorList);
    }
  }

  /**
   * Get Error log type into a list
   * @param RDD[CassLogModel]
   * @return List[CassLogMode] with length of 3
   */
  def filterError(rdd: RDD[CassLogModel]): List[String] = {
    val filteredRdd = rdd.filter(cassLogModel => {
      if (cassLogModel != null)
        cassLogModel.log_type == "ERROR"
      else
        false
    })

    val errorList = filteredRdd.take(3).toList
    logger.info(s"Error List: ${errorList}")
    errorList.map(clm => clm.log_message)
  }

  /**
   * Save RDD To Cassandra
   * @param rdd
   */
  def saveToCassandra(rdd: RDD[CassLogModel]): Unit = {
    cassLocation match {
      case "local" => throw new RuntimeException("Not Supported")
      case "aws" => saveToCassAws(rdd)
      case _ => throw new RuntimeException("Cassandra location not supported")
    }
  }

  /**
   * Loop Through RDD and save each CasslogModel into aws keyspace
   * @param rdd
   */
  def saveToCassAws(rdd: RDD[CassLogModel]): Unit = {
    rdd.foreach(cassLogModel => {
      if (cassLogModel != null) {
        logger.info(s"saving: ${cassLogModel}")
        save(cassLogModel)
      }
    })
  }

  /**
   * Save cassLogModel into aws keyspace using cql
   * @param cassLogModel
   */
  def save(cassLogModel: CassLogModel): Unit = {
    @transient val prepareStatement = session.prepare(
      s"""INSERT INTO ${config.getString("spark.cassandra.keyspace")}.${config.getString("spark.cassandra.table")}
        |   (log_id, file_name, log_message, log_type, timestamp) VALUES
        |   (?, ?, ?, ?, ?)""".stripMargin)
    @transient val parameterizedStatement = prepareStatement.bind(cassLogModel.log_id, cassLogModel.file_name, cassLogModel.log_message, cassLogModel.log_type, cassLogModel.timestamp)
    @transient val statement = parameterizedStatement.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
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
        parseToLogModel(logJson)
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
    logger.info(s"Topic: ${topics(0)}")
    stream
  }
}
