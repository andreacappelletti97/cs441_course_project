package AmazonSes

import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.services.simpleemail.model._


class EmailService {}

object EmailService extends App{

  val FROM = "andreacappelletti97@gmail.com"
  val TO = "andreacappelletti97@gmail.com"

  def sendEmail(subject: String, htmlBody: String, textBody: String): Unit ={
    val client = AmazonSimpleEmailServiceClientBuilder.standard().withRegion(Regions.US_WEST_1).build()
    val request = new SendEmailRequest()
      .withDestination(
        new Destination().withToAddresses(TO))
      .withMessage(new Message()
        .withBody(new Body()
          .withHtml(new Content()
            .withCharset("UTF-8").withData(htmlBody))
          .withText(new Content()
            .withCharset("UTF-8").withData(textBody)))
        .withSubject(new Content()
          .withCharset("UTF-8").withData(subject)))
      .withSource(FROM)

    client.sendEmail(request)
    println("Email sent!")
  }





}
