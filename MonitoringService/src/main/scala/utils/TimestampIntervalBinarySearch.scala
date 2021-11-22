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

  @tailrec
  private def binarySearchInner(start: LocalTime, end: LocalTime, logs: Vector[String], lineSplitter: String): Vector[String] = {
    val length: Int = logs.length

    if (length == 0) {
      return Vector.empty
    }

    val middleTime: LocalTime = LocalTime.parse(logs(length / 2).split(lineSplitter)(0))

    val isAfterTheStart: Boolean = middleTime.isAfter(start)
    val isBeforeTheEnd: Boolean = middleTime.isBefore(end)

    if (isAfterTheStart && isBeforeTheEnd) {
      // found time interval
      // we have to collect all the logs that are in the interval

      val beforeLogs: Vector[String] = findAllLogsBeforeOrAfter(logs, Vector.empty, (length / 2) - 1, start, end, Operation.FIND_BEFORE, lineSplitter)
      val afterLogs: Vector[String] = findAllLogsBeforeOrAfter(logs, Vector.empty, (length / 2) + 1, start, end, Operation.FIND_AFTER, lineSplitter)
      val foundLogs: Vector[String] = beforeLogs ++ Vector(logs(length / 2)) ++ afterLogs

      return foundLogs
    }

    else if (isAfterTheStart) {
      // we take the 1st half
      binarySearchInner(start, end, logs.slice(0, (length / 2) - 1), lineSplitter)
    }
    else {
      // we take the 2nd half
      binarySearchInner(start, end, logs.slice((length / 2) + 1, length), lineSplitter)
    }
  }

  private def findAllLogsBeforeOrAfter(logs: Vector[String], foundLogs: Vector[String], index: Int, start: LocalTime, end: LocalTime, operation: Operation, lineSplitter: String): Vector[String] = {
    if (index < 0 || index >= logs.length) {
      return foundLogs
    }

    val time: LocalTime = LocalTime.parse(logs(index).split(lineSplitter)(0))

    val isAfterTheStart: Boolean = time.isAfter(start)
    val isBeforeTheEnd: Boolean = time.isBefore(end)

    if (isAfterTheStart && isBeforeTheEnd) {
      val newLog: String = logs(index)
      val newFoundLogs: Vector[String] = foundLogs ++ Vector(newLog)
      val newIndex = if (operation == Operation.FIND_BEFORE) index -1 else index + 1
      return findAllLogsBeforeOrAfter(logs, newFoundLogs, newIndex, start, end, operation, lineSplitter)
    }

    return foundLogs
  }
}