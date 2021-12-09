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

class LogsProducer {

  def createLogRecord(receiver: ActorRef,logString: String) :Unit = {
    println("Sending logs to Kafka Cluster")

    val props:Properties = new Properties()
    props.put("bootstrap.servers","localhost:9092")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("acks","all")
    val producer = new KafkaProducer[String, String](props)
    val topic = "Violating Logs"
    try {
      val record = new ProducerRecord[String, String](topic, "key", logString)
      producer.send(record)
      println("The Kafka record has been created")
      receiver ! logString
    }catch{
      case e:Exception => e.printStackTrace()
    }finally {
      producer.close()
    }
  }
}

class LogsConsumer extends Actor {

  def receive: Receive = {
    case logs: String =>
      println("Running the Consumer to get the following logs:\n" +logs)
      println()
      val props:Properties = new Properties()
      props.put("group.id", "test")
      props.put("bootstrap.servers","localhost:9092")
      props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
      props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
      props.put("enable.auto.commit", "true")
      props.put("auto.commit.interval.ms", "1000")
      val consumer = new KafkaConsumer(props)
      val topics = List("Violating Logs")
      consumer.subscribe(topics.asJava)
      val records = consumer.poll(10)
      records.forEach(record => println(record))
      consumer.close()
  }
}

class FileMonitor(receiver: ActorRef) extends Actor {
  def receive: Receive = {
    case "Start" =>

      println("Starting log file monitoring")

      val s3: AmazonS3 = AmazonS3ClientBuilder.standard.withRegion(Regions.US_EAST_1).build
      val file = new File("/home/ec2-user/Project/LFG1/ProjectLFG/log/LogFileGenerator.2021-12-09.log")
      while(true) {

        val lines = scala.io.Source.fromFile(file).mkString

        val logs = lines.split("\n")
        val lastLine = logs(logs.length-1).split(" ")
        val secondLastLine = logs(logs.length-2).split(" ")
        val lastLogLevel = lastLine(2)
        val secondLastLogLevel = secondLastLine(2)
        if(lastLogLevel == "INFO" && secondLastLogLevel == "INFO" || lastLogLevel == "ERROR" && secondLastLogLevel == "WARN" || lastLogLevel == "ERROR" && secondLastLogLevel == "ERROR" || lastLogLevel == "WARN" && secondLastLogLevel == "WARN") {
          val logString = lastLine(0) + " " + secondLastLogLevel + " " + lastLine(lastLine.length-1) + "\n" + secondLastLine(0) + " " + lastLogLevel + " " + secondLastLine(secondLastLine.length-1)
          val obj = new LogsProducer
          obj.createLogRecord(receiver, logString)
          Thread.sleep(2500)
        }
      }
  }
}

object HelloAkka extends App {

  val system = ActorSystem("Actor-System")

  val receiver = system.actorOf(Props[LogsConsumer],"LogsReceiver")
  val monitor = system.actorOf(Props(new FileMonitor(receiver)),"FileMonitor")

  monitor ! "Start"
}