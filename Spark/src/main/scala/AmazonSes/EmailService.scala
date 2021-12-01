package AmazonSes

import Util.{CreateLogger, ObtainConfigReference}
import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.services.simpleemail.model._

import java.io._
import freemarker.template._
import scala.collection.JavaConverters._


class EmailService {}

object EmailService extends App{
  private val logger = CreateLogger(classOf[EmailService])
  private val config = ObtainConfigReference("emailService") match {
    case Some(value) => value
    case None => throw new RuntimeException("Can't obtain reference to the config")
  }

  //Setup the email sender and receiver
  val FROM = config.getString("emailService.from")
  val TO = config.getString("emailService.to")

  //Freemarker configuration
  val cfg = new Configuration
  val template = cfg.getTemplate(config.getString("emailService.emailTemplateDir"))

//  populateTemplate(List("Message1", "Message2", "Message3"))

  def populateTemplate(logData : List[String]): String ={
    val data = scala.collection.mutable.Map[String, Object]()
    data += ("message1" -> logData(0))
    data += ("message2" -> logData(1))
    data += ("message3" -> logData(2))

    // write to string
    val output = new StringWriter
    template.process(data.asJava, output)
    val stringResult = output.toString
    sendEmail(config.getString("emailService.subject"), stringResult, stringResult)
    stringResult
  }

  def sendEmail(subject: String, htmlBody: String, textBody: String): Unit ={
    val client = AmazonSimpleEmailServiceClientBuilder.standard().withRegion(Regions.US_WEST_1).build()
    val request = new SendEmailRequest()
      .withDestination(
        new Destination().withToAddresses(TO))
      .withMessage(new Message()
        .withBody(new Body()
          .withHtml(new Content()
            .withCharset(config.getString("emailService.charSet")).withData(htmlBody))
          .withText(new Content()
            .withCharset(config.getString("emailService.charSet")).withData(textBody)))
        .withSubject(new Content()
          .withCharset(config.getString("emailService.charSet")).withData(subject)))
      .withSource(FROM)

    client.sendEmail(request)
    println("Email sent!")
  }
}