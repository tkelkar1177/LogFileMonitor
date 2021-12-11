package Actors

import Actors.MonitorLogs.{receiver1, receiver2, system}
import Spark.GenerateMail
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.io.File
import java.time.Duration
import java.util.Properties
import scala.collection.JavaConverters._

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
    val record = new ProducerRecord[String, String](topic, "key", logString)
    producer.send(record)
    println("The Kafka record has been created with the following logs:\n"+logString)
    producer.close()
  }
}

class LogsConsumer extends Actor {

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
      val records = consumer.poll(Duration.ofSeconds(10)).asScala.mkString.split("value = ")(1).concat("\n")
      consumer.close()

      println("Sending the logs to the Spark app...")
      new GenerateMail().sendMail(records)
      println("Going back to monitoring...")
    case _ => println("Failed to Consume logs from the Kafka record")
  }
}

class FileMonitor(receiver: ActorRef, file: File) extends Actor {

  def receive: Receive = {
    case "Start" =>
      while(true) {
        Thread.sleep(2000)
        println("Starting log file monitoring for file: "+file)

        val linesSource = scala.io.Source.fromFile(file)
        val lines = linesSource.mkString
        linesSource.close()
        val logs = lines.split("\n")
        if(logs.length >=5) {
          val lastFiveLogs = logs(logs.length-1).concat("\n") + logs(logs.length-2).concat("\n") + logs(logs.length-3).concat("\n") + logs(logs.length-4).concat("\n") + logs(logs.length-5).concat("\n")
          new LogsProducer().createLogRecord(lastFiveLogs)
        }
        receiver ! "Consume"
      }
    case _ => println("Failed to start Actor system")
  }
}

object MonitorLogs extends App {

  val system = ActorSystem("Actor-System")

  val receiver1 = system.actorOf(Props[LogsConsumer],"LogsReceiver1")
  val receiver2 = system.actorOf(Props[LogsConsumer],"LogsReceiver2")
  val monitor1 = system.actorOf(Props(new FileMonitor(receiver1, new File("/home/ec2-user/Project/LFG1/ProjectLFG/log/LogFileGenerator.2021-12-11.log"))),"FileMonitor1")
  val monitor2 = system.actorOf(Props(new FileMonitor(receiver2, new File("/home/ec2-user/Project/LFG2/ProjectLFG/log/LogFileGenerator.2021-12-11.log"))),"FileMonitor2")

  println("Sending 'Start' message to the file monitor actor...")

  monitor1 ! "Start"
  monitor2 ! "Start"
}