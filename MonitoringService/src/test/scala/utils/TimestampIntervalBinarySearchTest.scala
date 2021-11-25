package utils

import com.typesafe.config.{Config, ConfigFactory}

import java.time.LocalTime
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TimestampIntervalBinarySearchTest extends AnyFlatSpec with Matchers {
  private val config: Config = ConfigFactory.load()

  it should "correctly find a log that is in the searched time window" in {
    val logs: Vector[String] = Vector(
      "11:44:27.233 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - test",
      "11:44:27.289 [scala-execution-context-global-14] INFO  HelperUtils.Parameters$ - test",
      "11:44:28.133 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - test",
      "11:44:29.343 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - test",
      "11:44:29.554 [scala-execution-context-global-14] ERROR  HelperUtils.Parameters$ - test",
      "11:44:30.122 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - test",
      "11:44:30.435 [scala-execution-context-global-14] INFO  HelperUtils.Parameters$ - test",
      "11:44:31.434 [scala-execution-context-global-14] INFO  HelperUtils.Parameters$ - test",
      "11:44:32.555 [scala-execution-context-global-14] DEBUG  HelperUtils.Parameters$ - test"
    )

    val start: LocalTime = LocalTime.parse("11:44:27")
    val end: LocalTime = LocalTime.parse("11:44:27.999")

    val results: Vector[String] = TimestampIntervalBinarySearch.binarySearch(start, end, logs, config)

    results.length shouldBe 2
    results(0) shouldBe "11:44:27.233 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - test"
    results(1) shouldBe "11:44:27.289 [scala-execution-context-global-14] INFO  HelperUtils.Parameters$ - test"
  }

  it should "return an empty vector if no logs are found in the specified time window" in {
    val logs: Vector[String] = Vector(
      "11:44:27.233 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - test",
      "11:44:27.289 [scala-execution-context-global-14] INFO  HelperUtils.Parameters$ - test",
      "11:44:28.133 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - test",
      "11:44:29.343 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - test",
      "11:44:29.554 [scala-execution-context-global-14] ERROR  HelperUtils.Parameters$ - test",
      "11:44:30.122 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - test",
      "11:44:30.435 [scala-execution-context-global-14] INFO  HelperUtils.Parameters$ - test",
      "11:44:31.434 [scala-execution-context-global-14] INFO  HelperUtils.Parameters$ - test",
      "11:44:32.555 [scala-execution-context-global-14] DEBUG  HelperUtils.Parameters$ - test"
    )

    val start: LocalTime = LocalTime.parse("11:44:30.500")
    val end: LocalTime = LocalTime.parse("11:44:30.999")

    val results: Vector[String] = TimestampIntervalBinarySearch.binarySearch(start, end, logs, config)

    results.length shouldBe 0
  }

  it should "return an empty vector if the specified time window isn't in logs" in {
    val logs: Vector[String] = Vector(
      "11:44:27.233 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - test",
      "11:44:27.289 [scala-execution-context-global-14] INFO  HelperUtils.Parameters$ - test",
      "11:44:28.133 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - test",
      "11:44:29.343 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - test",
      "11:44:29.554 [scala-execution-context-global-14] ERROR  HelperUtils.Parameters$ - test",
      "11:44:30.122 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - test",
      "11:44:30.435 [scala-execution-context-global-14] INFO  HelperUtils.Parameters$ - test",
      "11:44:31.434 [scala-execution-context-global-14] INFO  HelperUtils.Parameters$ - test",
      "11:44:32.555 [scala-execution-context-global-14] DEBUG  HelperUtils.Parameters$ - test"
    )

    val start: LocalTime = LocalTime.parse("11:50:30.500")
    val end: LocalTime = LocalTime.parse("11:50:30.999")

    val results: Vector[String] = TimestampIntervalBinarySearch.binarySearch(start, end, logs, config)

    results.length shouldBe 0
  }

  it should "return logs even if they are at the edges of the searched time window" in {
    val logs: Vector[String] = Vector(
      "11:44:27.233 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - test",
      "11:44:27.289 [scala-execution-context-global-14] INFO  HelperUtils.Parameters$ - test",
      "11:44:28.133 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - test",
      "11:44:29.343 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - test",
      "11:44:29.554 [scala-execution-context-global-14] ERROR  HelperUtils.Parameters$ - test",
      "11:44:30.122 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - test",
      "11:44:30.435 [scala-execution-context-global-14] INFO  HelperUtils.Parameters$ - test",
      "11:44:31.434 [scala-execution-context-global-14] INFO  HelperUtils.Parameters$ - test",
      "11:44:32.555 [scala-execution-context-global-14] DEBUG  HelperUtils.Parameters$ - test"
    )

    val start: LocalTime = LocalTime.parse("11:44:27.233")
    val end: LocalTime = LocalTime.parse("11:44:32.555")

    val results: Vector[String] = TimestampIntervalBinarySearch.binarySearch(start, end, logs, config)

    results.length shouldBe 9
    results(0) shouldBe "11:44:27.233 [scala-execution-context-global-14] WARN  HelperUtils.Parameters$ - test"
    results(8) shouldBe "11:44:32.555 [scala-execution-context-global-14] DEBUG  HelperUtils.Parameters$ - test"
  }
}
