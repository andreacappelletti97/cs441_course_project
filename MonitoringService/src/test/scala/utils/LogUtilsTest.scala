package utils

import org.scalatest.PrivateMethodTester
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LogUtilsTest extends AnyFlatSpec with Matchers with PrivateMethodTester{
  behavior of "LogUtils"

  "getLogMessage" should "Return the correct log message from the log input" in {
    val msg1 = "16:54:04.774 [scala-execution-context-global-25] ERROR HelperUtils.Parameters$ - s%]s,+2k|D}K7b/XCwG&@7HDPR8z"
    val msg2 = "16:54:05.301 [scala-execution-context-global-25] DEBUG HelperUtils.Parameters$ - JrQB;P0\"&+6;&Dk-"
    val ans1 = LogUtils.getLogMessage(msg1)
    val ans2 = LogUtils.getLogMessage(msg2)

    assert(ans1 == "s%]s,+2k|D}K7b/XCwG&@7HDPR8z")
    assert(ans2 == "JrQB;P0\"&+6;&Dk-")
  }

  "getLogTimeStamp" should "return timestamp string from the log message" in {
    val msg1 = "22:23:19.384 [scala-execution-context-global-25] ERROR HelperUtils.Parameters$ - N&I3aq7Wae3A9fQ5ice1V5k~=R{s6ng"
    val msg2 = "16:54:05.301 [scala-execution-context-global-25] DEBUG HelperUtils.Parameters$ - JrQB;P0\"&+6;&Dk-"

    val ans1 = LogUtils.getLogTimeStamp(msg1)
    val ans2 = LogUtils.getLogTimeStamp(msg2)

    assert(ans1.get == "22:23:19.384")
    assert(ans2.get == "16:54:05.301")
  }

  "getLogLevel" should "Return the correct log level from the log input" in {
    val msg1 = "16:54:04.774 [scala-execution-context-global-25] ERROR HelperUtils.Parameters$ - s%]s,+2k|D}K7b/XCwG&@7HDPR8z"
    val msg2 = "16:54:05.301 [scala-execution-context-global-25] DEBUG HelperUtils.Parameters$ - JrQB;P0\"&+6;&Dk-"
    val ans1 = LogUtils.getLogLevel(msg1)
    val ans2 = LogUtils.getLogLevel(msg2)

    assert(ans1 == "ERROR")
    assert(ans2 == "DEBUG")
  }

}
