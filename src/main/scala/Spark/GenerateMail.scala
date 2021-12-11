package Spark

import org.apache.spark.{SparkConf, SparkContext}

import java.io.File
import javax.mail._
import javax.mail.internet._

class GenerateMail {

  def sendMail(logs: String, file: File) :Unit = {

    println("Starting Spark...")

    val lines = List(logs)
    val errorCount = lines.flatMap(line => line.split(" ")).filter(x => x.equals("ERROR"))
    val warnCount = lines.flatMap(line => line.split(" ")).filter(x => x.equals("WARN"))

    if(errorCount.length >= 2 || warnCount.length >= 2 || errorCount.length + warnCount.length >= 2) {
      val props = System.getProperties
      props.setProperty("mail.smtp.host", "smtp.gmail.com")
      props.setProperty("mail.smtp.user","user")
      props.setProperty("mail.smtp.port", "587")
      props.setProperty("mail.debug", "true")
      props.setProperty("mail.smtp.auth", "true")
      props.setProperty("mail.smtp.starttls.enable","true")
      props.setProperty("mail.smtp.EnableSSL.enable","true")

      val session = Session.getInstance(props)
      val message = new MimeMessage(session)

      if(errorCount.length + warnCount.length >= 2) {
        val bodyText = errorCount.length + " ERROR logs and " + warnCount.length + " WARN logs were detected in the timestamp range: " + logs.split("\n")(0).split(" ")(0) + " - " + logs.split("\n")(4).split(" ")(0) + "in file:\n" + file
        message.setText(bodyText)
      }
      else if (errorCount.length >= 2) {
        val bodyText = errorCount.length + " ERROR logs were detected in the timestamp range: " + logs.split("\n")(0).split(" ")(0) + " - " + logs.split("\n")(4).split(" ")(0) + "in file:\n" + file
        message.setText(bodyText)
      }
      else {
        val bodyText = warnCount.length + " WARN logs were detected in the timestamp range: " + logs.split("\n")(0).split(" ")(0) + " - " + logs.split("\n")(4).split(" ")(0) + "in file:\n" + file
        message.setText(bodyText)
      }

      message.setFrom(new InternetAddress("tanmay.kelkar17@gmail.com"))
      message.setRecipients(Message.RecipientType.TO, "tanmay.kelkar17@gmail.com")
      message.setSubject("ERROR/WARN logs Detected!")

      println("Sending mail...")
      Transport.send(message, "tanmay.kelkar17@gmail.com", "Mir@ge12")
    }
    println("Returning to the Monitor application...")
  }
}