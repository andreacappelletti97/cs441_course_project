import Util.ObtainConfigReference
import Util.Utility.{kafkaConfig, parseToLogModel}
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.scalatest.PrivateMethodTester
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SparkUtilTest extends AnyFlatSpec with Matchers with PrivateMethodTester {
  behavior of "Spark-Stream Util"
  private val config = ObtainConfigReference("spark") match {
    case Some(value) => value
    case None => throw new RuntimeException("Can't obtain reference to the config")
  }

  it should "Successful parse string to json object" in {
    val json = "{ \"filename\": \"output1\", \"level\" : \"INFO\", \"message\" : \"B?y&C\\\"C5rsb:2037;f&|vM#x?z|Ny|&<44Z8B&rF1#&M\", \"timestamp\" : \"10:17:23.002\" }"
    val invalidJson = "{ \"sdf\": \"sdf\" }"

    println(json)
    val logModel = parseToLogModel(json)
    println(logModel)
    logModel should not equal null

    val logModel2 = parseToLogModel(invalidJson)
    logModel2 shouldEqual null
  }

  it should "Populate the configuration appropriately" in {
    val kafkaConf = kafkaConfig()

    kafkaConf("bootstrap.servers") shouldEqual config.getString("spark.boostrap-server")
    kafkaConf("auto.offset.reset") shouldEqual config.getString("spark.offset")
    kafkaConf("ssl.truststore.location") shouldEqual config.getString("spark.truststore-path")
  }
}
