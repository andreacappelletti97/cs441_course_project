package actors

import akka.actor.Actor
import com.redis.RedisClient
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.io.FileUtils
import org.slf4j.{Logger, LoggerFactory}
import utils.MonitoringMessageTypes

import java.io.File
import java.nio.file.StandardWatchEventKinds._
import java.nio.file._
import java.time.LocalTime
import scala.jdk.CollectionConverters._


case class LogMonitorMessage(messageType: MonitoringMessageTypes.MessageType, directoryPath: String = "", logFileName: String = "")

class LogMonitorActor extends Actor {

  private val config: Config = ConfigFactory.load()
  private val logger: Logger = LoggerFactory.getLogger(classOf[LogMonitorActor])
  private val redis: RedisClient = setupRedis()

  def receive(): Receive = {
    case m: LogMonitorMessage => onNewMessage(m)
    case _ => println("Message not recognized")
  }

  private def onNewMessage(logMonitorMessage: LogMonitorMessage): Unit = {
    logMonitorMessage.messageType match {
      case MonitoringMessageTypes.START => startMonitoring(logMonitorMessage.directoryPath, logMonitorMessage.logFileName)
    }
  }

  private def startMonitoring(directoryPath: String, logFileName: String): Unit = {
    initialCheckOnLogs(Path.of(directoryPath), logFileName)
    watchLogs(Path.of(directoryPath), logFileName)
  }

  // Used when the Akka actors are started in order to instantly get the latest log changes
  private def initialCheckOnLogs(directoryPath: Path, logFileName: String): Unit = {
    val filePath: String = s"${directoryPath.toString}/$logFileName"
    if(new File(filePath).exists()) {
      onNewLogs(filePath)
    }
  }

  @scala.annotation.tailrec
  private def watchLogs(directoryPath: Path, logFileName: String): Unit = {
    val service: WatchService = directoryPath.getFileSystem.newWatchService()
    directoryPath.register(service, ENTRY_CREATE, ENTRY_MODIFY)
    val watchKey: WatchKey = service.take()

    watchKey
      .pollEvents()
      .forEach(event => {
        event.kind() match {
          case ENTRY_CREATE | ENTRY_MODIFY =>
            logger.info("File Creation/Modification Detected...")
            val fileName: String = event.context().asInstanceOf[Path].getFileName.toString

            fileName match {
              case `logFileName` => onNewLogs(s"${directoryPath}/$fileName") // we only observe one file per actor
              case _ => logger.info("Not a file observed by this actor...")
            }
          case _ =>
            logger.warn("Event not recognized")
        }
      })

    watchKey.reset()
    watchLogs(directoryPath, logFileName)
  }


  private def onNewLogs(filePath: String): Unit = {
    // If more than one Time Interval

    //    val timeWindows = config.getObjectList("monitoringService.timeWindow").asScala
    //
    //    val timeIntervals: List[Map[String, LocalTime]] = timeWindows.map(timeInterval => {
    //      val interval = timeInterval.toConfig
    //      val startTime: LocalTime = LocalTime.parse(interval.getString("start"))
    //      val endTime: LocalTime = LocalTime.parse(interval.getString("end"))
    //
    //      Map.apply("start" -> startTime, "end" -> endTime)
    //    }).toList

    val timeWindow = config.getObject("monitoringService.timeWindow")
    val startTime: LocalTime = LocalTime.parse(timeWindow.toConfig.getString("start"))
    val endTime: LocalTime = LocalTime.parse(timeWindow.toConfig.getString("end"))

    val timeInterval: Map[String, LocalTime] = Map.apply("start" -> startTime, "end" -> endTime)
    val logFile: File = new File(filePath)

    val redisKey: String = s"${config.getString("monitoringService.redisKeyLastTimeStamp")}-${logFile.getName}"
    val lastTimestamp: String = redis.get(key = redisKey).orNull

    val firstTimestampToSearch: LocalTime = if (lastTimestamp != null) LocalTime.parse(lastTimestamp) else timeInterval("start")
    val skipFirstTimestamp: Boolean = lastTimestamp != null

    val lines: List[String] = FileUtils.readLines(logFile, "UTF-8").asScala.toList
    val lineSeparator: String =  config.getString("monitoringService.lineSeparator")

    val newLogs: List[String] = lines.filter(line => {
      val tokens = line.split(lineSeparator)
      val timestamp: LocalTime = LocalTime.parse(tokens(0))

      (timestamp.equals(firstTimestampToSearch) && !skipFirstTimestamp) ||
        timestamp.equals(timeInterval("end")) ||
        (timestamp.isAfter(firstTimestampToSearch) && timestamp.isBefore(timeInterval("end")))
    })

    if (newLogs.nonEmpty) {
      redis.set(key = redisKey, value = newLogs.last.split(lineSeparator)(0))
      // communication with Kafka component happens here
      newLogs.foreach(println)
    }
  }

  private def setupRedis(): RedisClient = {
    new RedisClient("localhost", 6379)
  }
}
