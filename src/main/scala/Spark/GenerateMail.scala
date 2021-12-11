package Spark

import org.apache.hadoop.shaded.org.checkerframework.checker.units.qual.K
import org.apache.spark.{SparkConf, SparkContext}

import java.util.Properties
import org.apache.kafka.clients.consumer.{ConsumerRecords, KafkaConsumer}

import scala.collection.JavaConverters._

class GenerateMail {

  def sendMail(logs: String) :Unit = {

    val conf = new SparkConf().setAppName("Logs aggregator")
      .setMaster("local")
    val sc = new SparkContext(conf)

    println("\n\n"+logs)
  }
}