package Model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}


case class LogModel(level: String, message: String, timestamp: String);

object LogModel {
  implicit val logDecoder: Decoder[LogModel] = deriveDecoder[LogModel]
  implicit val logEncoder: Encoder[LogModel] = deriveEncoder[LogModel]
}

