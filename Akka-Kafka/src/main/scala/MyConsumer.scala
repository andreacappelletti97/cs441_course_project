import akka.Done
import akka.actor.ActorSystem
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.Sink
import spray.json.{DefaultJsonProtocol, enrichAny}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import spray.json.DefaultJsonProtocol.{ByteJsonFormat, arrayFormat}

object MyConsumer extends App {
  implicit val actorSystem = ActorSystem("logs-consumer")

  val config = actorSystem.settings.config.getConfig("akka.kafka.consumer")
  val consumerSettings =
    ConsumerSettings(config, new StringDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers("localhost:9092")
      .withGroupId("group1")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val control: Consumer.DrainingControl[Done] = Consumer
    .sourceWithOffsetContext(consumerSettings, Subscriptions.topics("Logs")) // (5)
    .map { log => // (6)
      val logVal = log.value().toJson.prettyPrint
      println(logVal)
    }
    .toMat(Sink.ignore)(Consumer.DrainingControl.apply) // (10)
    .run()

}
