package Actors

import Actors.MonitorLogs.system
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.io.File
import java.util.Properties

class LogsProducer {

  val logMonitor: ActorRef = system.actorOf(Props[FileMonitor],"FileMonitor")

  def createLogRecord(logString: String) :Unit = {
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
      println("Sending the state back to monitoring...")
    }catch{
      case e:Exception => e.printStackTrace()
    }finally {
      producer.close()
      logMonitor ! "Monitor"
    }
  }
}

class FileMonitor extends Actor {

  val file = new File("/home/ec2-user/Project/LFG1/ProjectLFG/log/LogFileGenerator.2021-12-10.log")

  def receive: Receive = {
    case "Monitor" =>

      println("Scanning the log file...")

      val linesSource = scala.io.Source.fromFile(file)
      val lines = linesSource.mkString
      linesSource.close()
      val logs = lines.split("\n")
      val lastLine = logs(logs.length-1).split(" ")
      val secondLastLine = logs(logs.length-2).split(" ")
      val lastLogLevel = lastLine(2)
      val secondLastLogLevel = secondLastLine(2)
      if(lastLogLevel == "INFO" && secondLastLogLevel == "INFO" || lastLogLevel == "ERROR" && secondLastLogLevel == "WARN" || lastLogLevel == "ERROR" && secondLastLogLevel == "ERROR" || lastLogLevel == "WARN" && secondLastLogLevel == "WARN") {
        val logString = lastLine(0) + " " + secondLastLogLevel + " " + lastLine(lastLine.length-1) + "\n" + secondLastLine(0) + " " + lastLogLevel + " " + secondLastLine(secondLastLine.length-1)
        val obj = new LogsProducer
        println("Violating logs detected. Sending log info to Producer...")
        obj.createLogRecord(logString)
        Thread.sleep(2500)
      }
    case _ => println("Failed to run Actor system")
  }
}

object MonitorLogs extends App {

  val system = ActorSystem("Actor-System")

  val monitor = system.actorOf(Props[FileMonitor],"FileMonitorStarter")

  println("Sending 'Start' message to the file monitor actor...")

  monitor ! "Monitor"
}