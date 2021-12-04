package Util

import Model.{CassLogModel, KafkaLogModel}
import io.circe.parser.parse
import org.apache.kafka.common.serialization.StringDeserializer

import java.util.UUID

class Utility

object Utility {
  private val config = ObtainConfigReference("spark") match {
    case Some(value) => value
    case None => throw new RuntimeException("Can't obtain reference to the config")
  }

  /**
   * Parse String from stream to CassLogModel Type
   */
  def parseToLogModel(str: String): CassLogModel = {
    val logEither = parse(str)
    logEither match {
      case Right(logModel) => logModel.as[KafkaLogModel] match {
        case Right(logModel) => CassLogModel(UUID.randomUUID(), logModel.filename,logModel.message, logModel.level, logModel.timestamp)
        case Left(_) => null
      }
      case Left(_) => null
    }
  }

  /**
   * Kafka configuration
   * @return
   */
  def kafkaConfig(): Map[String, Object] = {
    Map[String, Object](
      "bootstrap.servers" -> s"${config.getString("spark.boostrap-server")}",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> config.getString("spark.groupId"),
      "auto.offset.reset" -> config.getString("spark.offset"),
      "enable.auto.commit" -> (false: java.lang.Boolean),
      "security.protocol" -> "SSL",
      "ssl.truststore.location" -> config.getString("spark.truststore-path"),
      "ssl.truststore.password" -> config.getString("spark.truststore-password")
    )
  }
}

