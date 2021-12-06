Author: Tanmay Sandeep Kelkar

I have worked on this Project alone.

This project consists of three stages:

1. LogFileGenerator

The LogFileGenerators run on EC2 instances. They run in real time and create a new log file in the same S3 bucket every time a new log is generated. This is because S3 does not allow modifications to the same file.

2. Actor System:

This can be run by running "sbt clean compile run" in the project directory.

The actor system consists of an actor that performs real time monitoring of the Log files that are updated in S3. When the actor sees that in the last two logs there are either:
  1. Two consecutive WARN logs
  2. Two consecutive ERROR logs
  3. A WARN followed by an ERROR log,

it invokes a Kafka Producer which then creates a Record under the Topic "Violating Logs" consisting of the log info which includes the timestamp, the log level and the randomly generated string. Another Actor invokes a Kafka Consumer, which sends the log info to an aggregator in Spark.

3. Spark system

This can be run by running "sbt clean compile run" in the project directory.

This has the primary task of collecting the violating logs and generating an email containing these logs which simply provides the timerange of the violation, along with the scenario (Two WARNS, or Two ERRORS, or a WARN and an ERROR) followed by the random string, which I assume to be the error message. I have set my UIC email id as the stakeholder id for this project.
