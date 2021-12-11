package Actors

import HelperUtils.{CreateLogger, ObtainConfigReference}
import Spark.GenerateMail
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.io.File
import java.time.Duration
import java.util.Properties
import scala.collection.JavaConverters._
import java.util.logging.Logger

class LogsProducer {

  val logger: Logger = Logger.getLogger(this.getClass.getName)

  val config: Config = ObtainConfigReference("logFileMonitor") match {
    case Some(value) => value
    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
  }

  def createLogRecord(logString: String) :Unit = {
    logger.info("Running the Producer to send the logs to create a Kafka record...")

    val props:Properties = new Properties()
    props.put(config.getString("logFileMonitor.properties.producer.bootstrapServers.key"), config.getString("logFileMonitor.properties.producer.bootstrapServers.value"))
    props.put(config.getString("logFileMonitor.properties.producer.keySerializer.key"), config.getString("logFileMonitor.properties.producer.keySerializer.value"))
    props.put(config.getString("logFileMonitor.properties.producer.valueSerializer.key"), config.getString("logFileMonitor.properties.producer.valueSerializer.value"))
    props.put(config.getString("logFileMonitor.properties.producer.acks.key"), config.getString("logFileMonitor.properties.producer.acks.value"))
    val producer = new KafkaProducer[String, String](props)
    val topic = config.getString("logFileMonitor.topic")
    val record = new ProducerRecord[String, String](topic, config.getString("logFileMonitor.key"), logString)
    producer.send(record)
    logger.info("The Kafka record has been created with the following logs:\n"+logString)
    producer.close()
  }
}

class LogsConsumer extends Actor {

  val config: Config = ObtainConfigReference("logFileMonitor") match {
    case Some(value) => value
    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
  }

  val logger: Logger = Logger.getLogger(this.getClass.getName)

  def receive: Receive = {
    case ("Consume", file: File) =>
      logger.info("Running the Consumer to get the logs...")

      val props: Properties = new Properties()
      props.put(config.getString("logFileMonitor.properties.consumer.groupId.key"), config.getString("logFileMonitor.properties.consumer.groupId.value"))
      props.put(config.getString("logFileMonitor.properties.consumer.bootstrapServers.key"), config.getString("logFileMonitor.properties.consumer.bootstrapServers.value"))
      props.put(config.getString("logFileMonitor.properties.consumer.keyDeserializer.key"), config.getString("logFileMonitor.properties.consumer.keyDeserializer.value"))
      props.put(config.getString("logFileMonitor.properties.consumer.valueDeserializer.key"), config.getString("logFileMonitor.properties.consumer.valueDeserializer.value"))
      props.put(config.getString("logFileMonitor.properties.consumer.autoCommit.key"), config.getString("logFileMonitor.properties.consumer.autoCommit.value"))
      props.put(config.getString("logFileMonitor.properties.consumer.autoCommitInterval.key"), config.getString("logFileMonitor.properties.consumer.autoCommitInterval.value"))
      val consumer = new KafkaConsumer(props)
      val topics = List(config.getString("logFileMonitor.topic"))
      consumer.subscribe(topics.asJava)
      val records = consumer.poll(Duration.ofSeconds(10)).asScala.mkString.split("value = ")(1).concat("\n")
      println(records)
      consumer.close()

      logger.info("Sending the logs to the Spark app...")
      new GenerateMail().sendMail(records, file)
      logger.info("Going back to monitoring...")
    case _ => logger.info("Failed to Consume logs from the Kafka record")
  }
}

class FileMonitor(receiver: ActorRef, file: File) extends Actor {

  val logger: Logger = Logger.getLogger(this.getClass.getName)

  def receive: Receive = {
    case "Start" =>
      while(true) {
        Thread.sleep(2000)
        logger.info("Starting log file monitoring for file: "+file)

        val linesSource = scala.io.Source.fromFile(file)
        val lines = linesSource.mkString
        linesSource.close()
        val logs = lines.split("\n")
        if(logs.length >=5) {
          val lastFiveLogs = logs(logs.length-1).concat("\n") + logs(logs.length-2).concat("\n") + logs(logs.length-3).concat("\n") + logs(logs.length-4).concat("\n") + logs(logs.length-5).concat("\n")
          new LogsProducer().createLogRecord(lastFiveLogs)
        }
        receiver ! ("Consume", file)
      }
    case _ => logger.info("Failed to start Actor system")
  }
}

object MonitorLogs extends App {

  val config: Config = ObtainConfigReference("logFileMonitor") match {
    case Some(value) => value
    case None => throw new RuntimeException("Cannot obtain a reference to the config data.")
  }

  val logger: Logger = Logger.getLogger(this.getClass.getName)

  val system = ActorSystem(config.getString("logFileMonitor.actorSystemName"))

  val receiver1 = system.actorOf(Props[LogsConsumer],config.getString("logFileMonitor.receiver1Name"))
  val receiver2 = system.actorOf(Props[LogsConsumer],config.getString("logFileMonitor.receiver2Name"))
  val monitor1 = system.actorOf(Props(new FileMonitor(receiver1, new File(config.getString("logFileMonitor.logFile1Path")))),config.getString("logFileMonitor.monitor1Name"))
  val monitor2 = system.actorOf(Props(new FileMonitor(receiver2, new File(config.getString("logFileMonitor.logFile2Path")))),config.getString("logFileMonitor.monitor2Name"))

  logger.info("Sending 'Start' message to the file monitor actor...")

  monitor1 ! "Start"
  monitor2 ! "Start"
}