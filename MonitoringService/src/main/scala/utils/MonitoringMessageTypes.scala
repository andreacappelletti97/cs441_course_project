package utils

/**
 * Types of messages that can be sent to the LogMonitorActor
 */
object MonitoringMessageTypes extends Enumeration {
  type MessageType = Value

  // Start -> start the monitoring of the log files
  val START: MonitoringMessageTypes.Value = Value
}

