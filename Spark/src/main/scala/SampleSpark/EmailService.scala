package SampleSpark

import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.model.{Body, Content, Destination, Message, SendEmailRequest}
import com.amazonaws.services.simpleemail.{AmazonSimpleEmailServiceClientBuilder}


class EmailService {}

object EmailService extends App{

  val FROM = "andreacappelletti97@gmail.com"
  val TO = "andreacappelletti97@gmail.com"
  val SUBJECT = "Amazon SES test (AWS SDK for Java)"
  val HTMLBODY = "<h1>Amazon SES test (AWS SDK for Java)</h1>" + "<p>This email was sent with <a href='https://aws.amazon.com/ses/'>" + "Amazon SES</a> using the <a href='https://aws.amazon.com/sdk-for-java/'>" + "AWS SDK for Java</a>";
  val TEXTBODY = "This email was sent through Amazon SES " + "using the AWS SDK for Java."

  val client = AmazonSimpleEmailServiceClientBuilder.standard().withRegion(Regions.US_WEST_1).build()

  val request = new SendEmailRequest()
      .withDestination(
        new Destination().withToAddresses(TO))
      .withMessage(new Message()
        .withBody(new Body()
          .withHtml(new Content()
            .withCharset("UTF-8").withData(HTMLBODY))
          .withText(new Content()
            .withCharset("UTF-8").withData(TEXTBODY)))
        .withSubject(new Content()
          .withCharset("UTF-8").withData(SUBJECT)))
      .withSource(FROM)

    client.sendEmail(request)
    println("Email sent!")

}
