package kafka

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.{Producer, SendProducer}
import akka.stream.scaladsl.{Flow, Source}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.{Logger, LoggerFactory}
import spray.json.DefaultJsonProtocol.jsonFormat3
import spray.json.{DefaultJsonProtocol, enrichAny}
import DefaultJsonProtocol._
import utils.LogUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MonitorKafkaProducer(implicit actorSystem: ActorSystem) {
  case class LogMessage(timestamp: String, level: String, message: String)
  implicit val logMessage = jsonFormat3(LogMessage)

  private val config: Config = ConfigFactory.load()
  private val logger: Logger = LoggerFactory.getLogger(classOf[MonitorKafkaProducer])
  private val producerConfig: Config =  config.getConfig("akka.kafka.producer")
  private val topic = config.getString("kafka.topic")

  val bootstrapServers = config.getString("kafka.bootstrapServers")
  val producerSettings =  ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)
    .withBootstrapServers(bootstrapServers)

  val producer = SendProducer(producerSettings)

  def publishToKafka(logs: Vector[String]): Unit ={
    val source = Source.fromIterator(() => logs.iterator)
    val mapToKafkaMessage: Flow[String,LogMessage,NotUsed] =  Flow[String].map[LogMessage](log => {
      LogMessage(getLogTimeStamp(log).getOrElse(""),getLogLevel(log),getLogMessage(log))
    })

    val mapLogToJson: Flow[LogMessage, String, NotUsed] = Flow[LogMessage].map[String](log => log.toJson.prettyPrint)

    val done: Future[Done] = source.via(mapToKafkaMessage)
      .via(mapLogToJson)
      .map(value => new ProducerRecord[String, String](topic, value))
      .runWith(Producer.plainSink(producerSettings))

    done onComplete {
      _ => logger.info("published logs to kafka")
    }

  }
}
