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

  def createLogRecord(receiver: ActorRef,logString: String) :Unit = {
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
      println("Sending 'Consume' message to the Consumer actor...")
      receiver ! "Consume"
    }catch{
      case e:Exception => e.printStackTrace()
    }finally {
      producer.close()
    }
  }
}

class LogsConsumer extends Actor {

  def receive: Receive = {
    case "Consume" =>
      println("Running the Consumer to get the logs...")
      val props:Properties = new Properties()
      props.put("group.id", "ViolatingLogs")
      props.put("bootstrap.servers","localhost:9092")
      props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
      props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
      props.put("enable.auto.commit", "true")
      props.put("auto.commit.interval.ms", "1000")
      val consumer = new KafkaConsumer(props)
      val topics = List("ViolatingLogs")
      consumer.subscribe(topics.asJava)
      consumer.poll(10)
      println("Logs consumed successfully. Sending over info to Spark application...\n")
      consumer.close()
    case _ => println("Failed to Consume logs from the Kafka record")
  }
}

class FileMonitor(receiver: ActorRef) extends Actor {
  def receive: Receive = {
    case "Start" =>

      println("Starting log file monitoring")

      val s3: AmazonS3 = AmazonS3ClientBuilder.standard.withRegion(Regions.US_EAST_1).build
      val file = new File("/home/tanmay/ProjectLFG/log/LogFileGenerator.2021-12-10.log")
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
          obj.createLogRecord(receiver, logString)
          Thread.sleep(2500)
        }
      }
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