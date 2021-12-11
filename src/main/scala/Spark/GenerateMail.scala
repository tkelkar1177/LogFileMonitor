package Spark

import HelperUtils.ObtainConfigReference
import com.typesafe.config.Config

import java.io.File
import java.util.logging.Logger
import javax.mail._
import javax.mail.internet._

class GenerateMail {

  val logger: Logger = Logger.getLogger(this.getClass.getName)

  val config: Config = ObtainConfigReference("logFileMonitor") match {
    case Some(value) => value
    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
  }

  def sendMail(logs: String, file: File) :Unit = {

    logger.info("Starting Spark...")

    val lines = List(logs)
    val errorCount = lines.flatMap(line => line.split(" ")).filter(x => x.equals(config.getString("logFileMonitor.error")))
    val warnCount = lines.flatMap(line => line.split(" ")).filter(x => x.equals(config.getString("logFileMonitor.warn")))

    if(errorCount.length >= 2 || warnCount.length >= 2 || (errorCount.nonEmpty && warnCount.nonEmpty)) {
      val props = System.getProperties
      props.setProperty(config.getString("logFileMonitor.properties.mail.smptHost.key"), config.getString("logFileMonitor.properties.mail.smptHost.value"))
      props.setProperty(config.getString("logFileMonitor.properties.mail.smtpUser.key"), config.getString("logFileMonitor.properties.mail.smtpUser.value"))
      props.setProperty(config.getString("logFileMonitor.properties.mail.smtpPort.key"), config.getString("logFileMonitor.properties.mail.smtpPort.value"))
      props.setProperty(config.getString("logFileMonitor.properties.mail.debug.key"), config.getString("logFileMonitor.properties.mail.debug.value"))
      props.setProperty(config.getString("logFileMonitor.properties.mail.smtpAuth.key"), config.getString("logFileMonitor.properties.mail.smtpAuth.value"))
      props.setProperty(config.getString("logFileMonitor.properties.mail.startTlsEnable.key"),config.getString("logFileMonitor.properties.mail.startTlsEnable.value"))
      props.setProperty(config.getString("logFileMonitor.properties.mail.enableSslEnable.key"),config.getString("logFileMonitor.properties.mail.enableSslEnable.value"))

      val session = Session.getInstance(props)
      val message = new MimeMessage(session)

      if(errorCount.nonEmpty && warnCount.nonEmpty) {
        val bodyText = errorCount.length + " ERROR logs and " + warnCount.length + " WARN logs were detected in the timestamp range: " + logs.split("\n")(0).split(" ")(0) + " - " + logs.split("\n")(4).split(" ")(0) + " in file:\n" + file
        message.setText(bodyText)
      }
      else if (errorCount.length >= 2) {
        val bodyText = errorCount.length + " ERROR logs were detected in the timestamp range: " + logs.split("\n")(0).split(" ")(0) + " - " + logs.split("\n")(4).split(" ")(0) + " in file:\n" + file
        message.setText(bodyText)
      }
      else {
        val bodyText = warnCount.length + " WARN logs were detected in the timestamp range: " + logs.split("\n")(0).split(" ")(0) + " - " + logs.split("\n")(4).split(" ")(0) + " in file:\n" + file
        message.setText(bodyText)
      }

      message.setFrom(new InternetAddress(config.getString("logFileMonitor.emailId")))
      message.setRecipients(Message.RecipientType.TO, config.getString("logFileMonitor.emailId"))
      message.setSubject("ERROR/WARN logs Detected!")

      logger.info("Sending mail...")
      Transport.send(message, config.getString("logFileMonitor.emailId"), config.getString("logFileMonitor.password"))
    }
    logger.info("Returning to the Monitor application...")
  }
}