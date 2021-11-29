package actors

import akka.actor.Actor
import com.redis.RedisClient
import com.typesafe.config.{Config, ConfigFactory}
import kafka.MonitorKafkaProducer
import org.apache.commons.io.FileUtils
import org.slf4j.{Logger, LoggerFactory}
import utils.{MonitoringMessageTypes, TimestampIntervalBinarySearch}

import java.io.File
import java.nio.file.StandardWatchEventKinds._
import java.nio.file._
import java.time.LocalTime
import scala.jdk.CollectionConverters._

/**
 * Case class for messages sent to the LogMonitorActor from the main class
 *
 * @param messageType the type of the message sent to the Actor
 * @param directoryPath Path of the directory we are listening to
 * @param logFileName Name of the file we are actually monitoring
 */
case class LogMonitorMessage(messageType: MonitoringMessageTypes.MessageType, directoryPath: String = "", logFileName: String = "")

/**
 * Class that implements the Actor which is monitoring the log files and publishing them to Kafka
 */
class LogMonitorActor extends Actor {

  private val config: Config = ConfigFactory.load()
  private val logger: Logger = LoggerFactory.getLogger(classOf[LogMonitorActor])
  private val redis: RedisClient = setupRedis(config)
  private val kafka: MonitorKafkaProducer = new MonitorKafkaProducer(context.system)

  /**
   * Receives messages from the main class
   */
  def receive(): Receive = {
    case m: LogMonitorMessage => onNewMessage(m)
    case _ => logger.warn("Message not recognized")
  }

  /**
   * Called to perform actions in response to messages received
   * @param logMonitorMessage the received message
   */
  private def onNewMessage(logMonitorMessage: LogMonitorMessage): Unit = {
    logMonitorMessage.messageType match {
      case MonitoringMessageTypes.START => startMonitoring(logMonitorMessage.directoryPath, logMonitorMessage.logFileName)
    }
  }

  /**
   * Check if there are already new logs to be analyzed, then start to monitor log files
   * @param directoryPath Path of the directory we are listening to
   * @param logFileName Name of the file we are actually monitoring
   */
  private def startMonitoring(directoryPath: String, logFileName: String): Unit = {
    initialCheckOnLogs(Paths.get(directoryPath), logFileName)
    watchLogs(Paths.get(directoryPath), logFileName)
  }

  /**
   * Used when the Akka actors are started in order to instantly get the latest log changes
   * @param directoryPath Path of the directory we are listening to
   * @param logFileName Name of the file we are actually monitoring
   */
  private def initialCheckOnLogs(directoryPath: Path, logFileName: String): Unit = {
    val filePath: String = s"${directoryPath.toString}/$logFileName"
    if(new File(filePath).exists()) {
      onNewLogs(filePath)
    }
  }

  /**
   * Monitors log files
   * @param directoryPath Path of the directory we are listening to
   * @param logFileName Name of the file we are actually monitoring
   */
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
              case `logFileName` => onNewLogs(s"$directoryPath/$fileName") // we only observe one file per actor
              case _ => logger.info("Not a file observed by this actor...")
            }
          case _ =>
            logger.warn("Event not recognized")
        }
      })

    watchKey.reset()
    watchLogs(directoryPath, logFileName)
  }

  /**
   * Method call when there are some modifications in the log file that we are listening to
   * @param filePath Path of the file we are monitoring
   */
  private def onNewLogs(filePath: String): Unit = {
    val logFile: File = new File(filePath)
    val redisKey: String = s"${config.getString("monitoringService.redisKeyLastTimeStamp")}-${logFile.getName}"
    val lastTimestamp: String = redis.get(key = redisKey).orNull
    val lineSeparator: String = config.getString("monitoringService.lineSeparator")
    val lines: Vector[String] = FileUtils.readLines(logFile, "UTF-8").asScala.toVector

    if (lines.isEmpty) {
      // The file has been created but it's empty
      return
    }

    val singleTimeWindow = config.getBoolean("monitoringService.singleTimeWindow")

    // If we have to consider a single time window for all the log files, we pick the first time window in config
    val indexOfListener: Int = if(singleTimeWindow) 0 else logFile.getName.charAt(logFile.getName.indexOf(".log") - 1).asDigit - 1
    val timeWindows = config.getObjectList("monitoringService.timeWindows").asScala
    val timeInterval = timeWindows(indexOfListener)

    val startTime: LocalTime = LocalTime.parse(timeInterval.toConfig.getString("start"))
    val endTime: LocalTime = LocalTime.parse(timeInterval.toConfig.getString("end"))

    val firstTimestampToSearch: LocalTime = if (lastTimestamp != null) LocalTime.parse(lastTimestamp) else startTime

    val newLogs: Vector[String] = TimestampIntervalBinarySearch.binarySearch(firstTimestampToSearch, endTime, lines, config)

    if (newLogs.nonEmpty) {
      redis.set(key = redisKey, value = newLogs.last.split(lineSeparator)(0))
      // communication with Kafka component happens here
      kafka.publishToKafka(newLogs)
    }
  }

  /**
   * Setup Redis DB connection
   * @param config configuration we are using to have configurable host and port
   * @return the created Redis Client
   */
  private def setupRedis(config: Config): RedisClient = {
    new RedisClient(config.getString("monitoringService.redisHost"), config.getInt("monitoringService.redisPort"))
  }
}
