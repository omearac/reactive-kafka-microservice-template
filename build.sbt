import Dependencies._

name := "reactive-kafka-microservice-template"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  akka_http_core,
  akka_http_testkit,
  akka_testkit,
  akka_slf4j,
  kafka,
  logback,
  log4j_over_slf4j,
  io_spray,
  play_json,
  scalatest
)

//Run tests Sequentially
parallelExecution in Test := false