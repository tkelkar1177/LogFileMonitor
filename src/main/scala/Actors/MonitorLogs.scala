package Actors

import Actors.MonitorLogs.{receiver, system}
import Spark.GenerateMail
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.io.File
import java.time.Duration
import java.util.Properties
import scala.collection.JavaConverters._

class LogsProducer {

  def createLogRecord(logMonitor: ActorRef ,logString: String) :Unit = {
    println("Running the Producer to send the logs to create a Kafka record...")

    val props:Properties = new Properties()
    props.put("bootstrap.servers","localhost:9092")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("acks","all")
    val producer = new KafkaProducer[String, String](props)
    val topic = "ViolatingLogs"
    try {
      val record = new ProducerRecord[String, String](topic, "key", logString)
      producer.send(record)
      println("The Kafka record has been created with the following logs:\n"+logString)
    }catch {
      case e:Exception => e.printStackTrace()
    }finally {
      producer.close()
      logMonitor ! "Consume"
    }
  }
}

class LogsConsumer extends Actor {

  val logsMonitor: ActorRef = system.actorOf(Props(new FileMonitor(receiver)),"LogFileMonitor")

  def receive: Receive = {
    case "Consume" =>
      println("Running the Consumer to get the logs...")

      val props: Properties = new Properties()
      props.put("group.id", "ViolatingLogs")
      props.put("bootstrap.servers", "localhost:9092")
      props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
      props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
      props.put("enable.auto.commit", "true")
      props.put("auto.commit.interval.ms", "10")
      val consumer = new KafkaConsumer(props)
      val topics = List("ViolatingLogs")
      consumer.subscribe(topics.asJava)
      val records = consumer.poll(Duration.ofSeconds(10)).asScala.mkString.concat("\n")
      consumer.close()

      println("Sending the logs to the Spark app...")
      new GenerateMail().sendMail(records)
      println("Going back to monitoring...")
      logsMonitor ! "Start"
    case _ => println("Failed to Consume logs from the Kafka record")
  }
}

class FileMonitor(receiver: ActorRef) extends Actor {

  val file = new File("/home/ec2-user/Project/LFG1/ProjectLFG/log/LogFileGenerator.2021-12-11.log")

  def receive: Receive = {
    case "Start" =>

      println("Starting log file monitoring")

      val linesSource = scala.io.Source.fromFile(file)
      val lines = linesSource.mkString
      linesSource.close()
      val logs = lines.split("\n")
      if(logs.length >=5) {
        val lastFiveLogs = logs(logs.length-1).concat("\n") + logs(logs.length-2).concat("\n") + logs(logs.length-3).concat("\n") + logs(logs.length-4).concat("\n") + logs(logs.length-5).concat("\n")
        new LogsProducer().createLogRecord(receiver, lastFiveLogs)
      }
      /*val lastLine = logs(logs.length-1).split(" ")
      val secondLastLine = logs(logs.length-2).split(" ")
      val lastLogLevel = lastLine(2)
      val secondLastLogLevel = secondLastLine(2)
      if(lastLogLevel == "INFO" && secondLastLogLevel == "INFO" || lastLogLevel == "ERROR" && secondLastLogLevel == "WARN" || lastLogLevel == "ERROR" && secondLastLogLevel == "ERROR" || lastLogLevel == "WARN" && secondLastLogLevel == "WARN") {
        val logString = lastLine(0) + " " + secondLastLogLevel + " " + lastLine(lastLine.length - 1) + "\n" + secondLastLine(0) + " " + lastLogLevel + " " + secondLastLine(secondLastLine.length - 1)
        val obj = new LogsProducer
        println("Violating logs detected. Sending log info to Producer actor...")
        obj.createLogRecord(receiver, logString)
      }*/
    case _ => println("Failed to start Actor system")
  }
}

object MonitorLogs extends App {

  val system = ActorSystem("Actor-System")

  val receiver = system.actorOf(Props[LogsConsumer],"LogsReceiver")
  val monitor = system.actorOf(Props(new FileMonitor(receiver)),"FileMonitor")

  println("Sending 'Start' message to the file monitor actor...")

  monitor ! "Start"
}