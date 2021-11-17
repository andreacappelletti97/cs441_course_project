import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.{Producer, SendProducer}
import akka.stream.scaladsl.{Flow, Source}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import spray.json.{DefaultJsonProtocol, enrichAny}
import DefaultJsonProtocol._
import Utils.LogUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

object Publisher extends App{

  case class LogMessage(timestamp: String, level: String, message: String)
  implicit val logMessage = jsonFormat3(LogMessage)
  implicit val actorSystem = ActorSystem("logs-actor")

  val bootstrapServers = "localhost:9092";
  val config = actorSystem.settings.config.getConfig("akka.kafka.producer")
  val producerSettings =  ProducerSettings(config, new StringSerializer, new StringSerializer)
    .withBootstrapServers(bootstrapServers)

  val producer = SendProducer(producerSettings)

  val fileName = "log/LogFileGenerator.2021-11-02.log"

  val log = scala.io.Source.fromFile(fileName)

  val source = Source.fromIterator(() => log.getLines());

  val mapToKafkaMessage: Flow[String,LogMessage,NotUsed] =  Flow[String].map[LogMessage](log => {
    LogMessage(getLogTimeStamp(log).getOrElse(""),getLogLevel(log),getLogMessage(log))
  })

  val mapLogToJson: Flow[LogMessage, String, NotUsed] = Flow[LogMessage].map[String](log => log.toJson.prettyPrint)

  val done: Future[Done] = source.via(mapToKafkaMessage)
    .via(mapLogToJson)
    .map(value => new ProducerRecord[String, String]("Logs", value))
    .runWith(Producer.plainSink(producerSettings))

  done onComplete {
    _ => print("published logs to kafka")
  }


}
