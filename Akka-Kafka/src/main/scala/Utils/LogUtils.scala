package Utils

import java.util.StringTokenizer
import scala.util.matching.Regex

object LogUtils {
  /**
   *Checks if the input string is a valid Log type or not
   * @param token
   * @return
   */
  def isValidLogLevel (token:String): Boolean= token match {
    case "DEBUG" => true
    case "INFO" => true
    case "WARN" => true
    case "ERROR" => true
    case _ => false
  }

  /**
   * Get the log level if it exists in the string passed
   * @param token
   * @return String
   */
  def getLogLevel (token:String): String ={
    val tokens = new StringTokenizer(token," ")
    getIfLogLevel(tokens)
  }

  /**
   * Recursively loop through a list of string tokens and determine if any of them are valid log types
   * returns a valid log type if it exists in the list of tokens
   * @param tokens
   * @return
   */
  def getIfLogLevel(tokens: StringTokenizer): String = {
    if(!tokens.hasMoreTokens()){
      return ""
    }
    val token: String = tokens.nextToken()
    if(isValidLogLevel(token)){
      return token
    }
    getIfLogLevel(tokens)
  }

  /**
   * Get the message part of the log string
   * @param log
   * @return
   */
  def getLogMessage(log: String): String = {
    val parts: Array[String] = log.split(" - ").map(str => str.trim())
    val message = parts(parts.length -1)
    message
  }

  /**
   * Retreive the timestamp string from the Log String
   * Uses a regex pattern to get the required timestamp
   * @param message
   * @return
   */
  def getLogTimeStamp(message:String): Option[String] = {
    // Regex pattern to match the time stamp in a log string
    val pattern = new Regex("[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}")
    pattern.findFirstIn(message)
  }
}
