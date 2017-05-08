object Dependencies {
  val kafka = "com.typesafe.akka" %% "akka-stream-kafka" % Versions.akka

  val logback = "ch.qos.logback" % "logback-classic" % Versions.logback

  val log4j_over_slf4j = "org.slf4j" % "log4j-over-slf4j" % Versions.log4j_over_slf4j

  val akka_slf4j = "com.typesafe.akka" %% "akka-slf4j" % Versions.akka

  val scalatest = "org.scalatest" %% "scalatest" % Versions.scalatest % "test"

  val akka_testkit = "com.typesafe.akka" %% "akka-testkit" % Versions.akka % "test"

  val play_json = "com.typesafe.play" % "play-json_2.11" % Versions.play_json

  val akka_http_core = "com.typesafe.akka" %% "akka-http-core" % Versions.akka_http

  val akka_http_testkit = "com.typesafe.akka" %% "akka-http-testkit" % Versions.akka_http

  val io_spray = "io.spray" %% "spray-json" % Versions.io_spray
}