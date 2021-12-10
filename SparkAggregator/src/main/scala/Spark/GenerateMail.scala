package Spark

import org.apache.spark.{SparkConf, SparkContext}

import java.util.Properties
import org.apache.kafka.clients.consumer.{ConsumerRecords, KafkaConsumer}

import scala.collection.JavaConverters._

object GenerateMail extends App{

  def aggregateLogs() :Unit = {

    val conf = new SparkConf().setAppName("Logs aggregator")
                              .setMaster("local")
    val sc = new SparkContext(conf)

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
    while(true) {
      val records = consumer.poll(10000)
      records.forEach(record => println(record))
    }
  }

}
