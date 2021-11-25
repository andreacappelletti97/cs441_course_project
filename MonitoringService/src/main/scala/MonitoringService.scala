import actors.{LogMonitorActor, LogMonitorMessage}
import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}
import utils.MonitoringMessageTypes

/**
 * This is the main class of the Monitoring Service that instantiate the Actors to monitor log files (one Actor for each log file)
 */
object MonitoringService extends App {
  private val config: Config = ConfigFactory.load()
  private val logger: Logger = LoggerFactory.getLogger(classOf[MonitoringService.type])

  val monitoringActorSystem = ActorSystem("MonitoringActorSystem")
  val numOfLogGeneratorInstances: Int = config.getInt("monitoringService.numOfLogGeneratorInstances")
  val basePath: String = config.getString("monitoringService.basePath")

  val range = 1 to numOfLogGeneratorInstances

  logger.info(s"Creating $numOfLogGeneratorInstances actors...")

  val monitoringActorRefs: List[ActorRef] = range.map(i => {
    val monitoringActorRef: ActorRef = monitoringActorSystem.actorOf(Props[LogMonitorActor], name = s"MonitoringActor_$i")
    monitoringActorRef.tell(LogMonitorMessage(MonitoringMessageTypes.START, directoryPath = basePath, logFileName = s"output$i.log"), ActorRef.noSender)
    monitoringActorRef
  }).toList
}