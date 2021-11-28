package Model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}


case class KafkaLogModel(level: String, message: String, timestamp: String);

object KafkaLogModel {
  implicit val logDecoder: Decoder[KafkaLogModel] = deriveDecoder[KafkaLogModel]
  implicit val logEncoder: Encoder[KafkaLogModel] = deriveEncoder[KafkaLogModel]
}

