package utils

import com.typesafe.config.Config
import utils.Operation.Operation

import java.time.LocalTime
import scala.annotation.tailrec

object Operation extends Enumeration {
  type Operation = Value

  val FIND_BEFORE, FIND_AFTER : Operation.Value = Value
}

object TimestampIntervalBinarySearch {

  /**
   * Public method called by the monitoring service to search for logs in a specific time window
   * @param start start of the time interval
   * @param end end of the time interval
   * @param logs array of logs retrieved from file
   * @param config configuration object
   * @return the Vector of found logs
   */
  def binarySearch(start: LocalTime, end: LocalTime, logs: Vector[String], config: Config): Vector[String] = {

    val length: Int = logs.length
    val lineSplitter: String = config.getString("monitoringService.lineSeparator")
    val firstTimestamp: LocalTime = LocalTime.parse(logs(0).split(lineSplitter)(0))
    val lastTimestamp: LocalTime = LocalTime.parse(logs(length - 1).split(lineSplitter)(0))

    if (end.isBefore(firstTimestamp) || start.isAfter(lastTimestamp)) {
      return Vector.empty
    }

    // The searched timestamp could be in the logs
    val foundLogs: Vector[String] = binarySearchInner(start, end, logs, lineSplitter)

    foundLogs.sortWith((log1, log2) => {
      val timestamp1: LocalTime = LocalTime.parse(log1.split(lineSplitter)(0))
      val timestamp2: LocalTime = LocalTime.parse(log2.split(lineSplitter)(0))
      timestamp1.isBefore(timestamp2)
    })
  }

  /**
   * Inner recursive function that performs the actual Binary Search on the logs Vector
   * @param start start of the time interval
   * @param end end of the time interval
   * @param logs array of logs retrieved from file
   * @param lineSplitter log line separator char
   * @return the Vector of found logs
   */
  @tailrec
  private def binarySearchInner(start: LocalTime, end: LocalTime, logs: Vector[String], lineSplitter: String): Vector[String] = {
    val length: Int = logs.length

    if (length == 0) {
      return Vector.empty
    }

    val middleTime: LocalTime = LocalTime.parse(logs(length / 2).split(lineSplitter)(0))

    if (isInInterval(middleTime, start, end)) {
      // found time interval
      // we have to collect all the logs that are in the interval

      val beforeLogs: Vector[String] = findAllLogsBeforeOrAfter(logs, Vector.empty, (length / 2) - 1, start, end, Operation.FIND_BEFORE, lineSplitter)
      val afterLogs: Vector[String] = findAllLogsBeforeOrAfter(logs, Vector.empty, (length / 2) + 1, start, end, Operation.FIND_AFTER, lineSplitter)
      val foundLogs: Vector[String] = beforeLogs ++ Vector(logs(length / 2)) ++ afterLogs

      foundLogs
    }

    else if (middleTime.isAfter(start)) {
      // we take the 1st half
      binarySearchInner(start, end, logs.slice(0, (length / 2) - 1), lineSplitter)
    }
    else {
      // we take the 2nd half
      binarySearchInner(start, end, logs.slice((length / 2) + 1, length), lineSplitter)
    }
  }

  /**
   * Inner recursive function that is called once a log that is in the searched time interval is found. This function is needed to get all the other logs that are
   * in the time interval, both those that are before the first log found and those that are after it in the log file.
   * @param logs array of logs retrieved from file
   * @param foundLogs a Vector containing only the lines of the log that are in the specified time interval, updated at every recursive call
   * @param index current index in the log Vector
   * @param start start of the time interval
   * @param end end of the time interval
   * @param operation the Operation we are currently performing
   * @param lineSplitter log line separator char
   * @return the found logs that are in the right time interval before or after the starting index
   */
  private def findAllLogsBeforeOrAfter(logs: Vector[String], foundLogs: Vector[String], index: Int, start: LocalTime, end: LocalTime, operation: Operation, lineSplitter: String): Vector[String] = {
    if (index < 0 || index >= logs.length) {
      return foundLogs
    }

    val time: LocalTime = LocalTime.parse(logs(index).split(lineSplitter)(0))

    if (isInInterval(time, start, end)) {
      val newLog: String = logs(index)
      val newFoundLogs: Vector[String] = foundLogs ++ Vector(newLog)
      val newIndex = if (operation == Operation.FIND_BEFORE) index -1 else index + 1
      return findAllLogsBeforeOrAfter(logs, newFoundLogs, newIndex, start, end, operation, lineSplitter)
    }

    foundLogs
  }

  /**
   * Utility function used to check if a time is contained in the time window delimited by START and END times
   * @param time the time that we want to check
   * @param start start of the time interval
   * @param end end of the time interval
   * @return true if TIME is in the time interval, false otherwise
   */
  private def isInInterval(time: LocalTime, start: LocalTime, end: LocalTime): Boolean = {
    val isStart: Boolean = time.equals(start)
    val isEnd: Boolean = time.equals(end)

    val isAfterTheStart: Boolean = time.isAfter(start)
    val isBeforeTheEnd: Boolean = time.isBefore(end)

    isStart || isEnd || (isAfterTheStart && isBeforeTheEnd)
  }
}