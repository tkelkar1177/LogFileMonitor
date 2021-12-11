name := "AkkaWithKafkaStreaming"

version := "0.1"

scalaVersion := "2.13.7"

val AkkaVersion = "2.6.17"
val awsJavaSdkS3 = "1.12.112"
val akkaStreamKafka = "2.1.1"
val log4jApi = "2.14.1"
val log4jCore = "2.14.1"
val log4jSlf4jImpl = "2.14.1"
val abc = "2.14.1"
val slf4j = "1.2.17"
val sparkCore = "3.2.0"
val mail = "1.5.0-b01"

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
  "com.amazonaws" % "aws-java-sdk-s3" % awsJavaSdkS3,
  "com.typesafe.akka" %% "akka-stream-kafka" % akkaStreamKafka,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "org.apache.logging.log4j" % "log4j-api" % log4jApi,
  "org.apache.logging.log4j" % "log4j-core" % log4jCore,
  "log4j" % "log4j" % slf4j,
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % abc,
  "org.apache.spark" %% "spark-core" % sparkCore,
  "javax.mail" % "mail" % mail
)