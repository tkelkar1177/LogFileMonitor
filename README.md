Author: Tanmay Sandeep Kelkar

I have worked on this Project alone.

Note: The Gmail credentials are temporary, and can be changed in the config file.

The entire project can be run by simply running "sbt clean compile run" in projects root directory. The tests can be run by running "sbt clean compile test".
This project consists of three stages:

1. LogFileGenerators

There are 2 LogFileGenerator instances. These run on EC2 instances. They run in real time and create a new log file in the 'log' folder in the project's root directory.

2. Actor System:

This can be run by running "sbt clean compile run" in the project directory.

The actor system consists of an actor that performs real time monitoring of the log files. When the actor sees that in the last two logs there are either:
  1. Two or more WARN logs in a time interval of 10 seconds
  2. Two or more ERROR logs in a time interval of 10 seconds
  3. Atleast one each of WARN and ERROR logs in a time interval of 10 seconds

it invokes a Kafka Producer which then creates a Record under the Topic "ViolatingLogs" consisting of the log info which includes the timestamp, the log level and the randomly generated string. Another Actor invokes a Kafka Consumer, which sends the log info to an aggregator in Spark.

3. Spark system

This has the primary task of collecting the violating logs and generating an email containing these logs which simply provides the timerange of the violation, along with the scenario (Atleast two WARNS, or Atleast two ERRORS, or Atleast a WARN and an ERROR). I have set my personal email id as the stakeholder id for this project.

The project is run on an EC2 instance.
