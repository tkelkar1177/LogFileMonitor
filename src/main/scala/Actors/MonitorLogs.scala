package Actors

import java.util.logging.Logger
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.kafka.ProducerSettings
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}

import java.util.Properties
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.clients.consumer.KafkaConsumer

import scala.collection.JavaConverters._
import java.io.{File, FileInputStream}
import java.time.Duration

class LogsProducer {

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
      println("The Spark app will consume these logs now...")
    }catch{
      case e:Exception => e.printStackTrace()
    }finally {
      producer.close()
    }
  }
}

class FileMonitor() extends Actor {
  def receive: Receive = {
    case "Start" =>

      println("Starting log file monitoring")

      val s3: AmazonS3 = AmazonS3ClientBuilder.standard.withRegion(Regions.US_EAST_1).build
      val file = new File("/home/ec2-user/Project/LFG1/ProjectLFG/log/LogFileGenerator.2021-12-10.log")
      while(true) {

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
          println("Violating logs detected. Sending log info to Producer actor...")
          obj.createLogRecord(logString)
          Thread.sleep(2500)
        }
      }
    case _ => println("Failed to start Actor system")
  }
}

object MonitorLogs extends App {

  val system = ActorSystem("Actor-System")

  val monitor = system.actorOf(Props[FileMonitor],"FileMonitor")

  println("Sending 'Start' message to the file monitor actor...")

  monitor ! "Start"
}