package Model

import java.util.UUID

case class CassLogModel(log_id: UUID, log_message: String, log_type: String, timestamp: String);
