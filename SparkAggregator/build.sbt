name := "SparkAggregator"

version := "0.1"

scalaVersion := "2.13.7"

val sparkCore = "3.2.0"
val akkaStreamKafka = "2.1.1"

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkCore,
  "com.typesafe.akka" %% "akka-stream-kafka" % akkaStreamKafka
)