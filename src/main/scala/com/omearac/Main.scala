package com.omearac

import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import com.omearac.consumers.ConsumerStreamManager.Messages.InitializeConsumerStream
import com.omearac.consumers.{ConsumerStreamManager, DataConsumer, EventConsumer}
import com.omearac.http.HttpService
import com.omearac.producers.ProducerStreamManager.Messages.InitializeProducerStream
import com.omearac.producers.{DataProducer, EventProducer, ProducerStreamManager}
import com.omearac.settings.Settings
import com.omearac.shared.AkkaStreams
import com.omearac.shared.KafkaMessages.{ExampleAppEvent, KafkaMessage}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn

/**
  * This starts the Reactive Kafka Microservice Template
  */

object Main extends App with HttpService with AkkaStreams {
    implicit val system = ActorSystem("akka-reactive-kafka-app")
    val log = Logging(system, this.getClass.getName)

    //Start the akka-http server and listen for http requests
    val akkaHttpServer = startAkkaHTTPServer()

    //Create the Producer Stream Manager and Consumer Stream Manager
    val producerStreamManager = system.actorOf(Props(new ProducerStreamManager), "producerStreamManager")
    val consumerStreamManager = system.actorOf(Props(new ConsumerStreamManager), "consumerStreamManager")

    //Create actor to publish event messages to kafka stream.
    val eventProducer = system.actorOf(Props(new EventProducer), "eventProducer")
    producerStreamManager ! InitializeProducerStream(eventProducer, ExampleAppEvent)

    //Create actor to consume event messages from kafka stream.
    val eventConsumer = system.actorOf(Props(new EventConsumer), "eventConsumer")
    consumerStreamManager ! InitializeConsumerStream(eventConsumer, ExampleAppEvent)

    //Create actor to publish data messages to kafka stream.
    val dataProducer = system.actorOf(Props(new DataProducer), "dataProducer")
    producerStreamManager ! InitializeProducerStream(dataProducer, KafkaMessage)

    //Create actor to consume data messages from kafka stream.
    val dataConsumer = system.actorOf(Props(new DataConsumer), "dataConsumer")
    consumerStreamManager ! InitializeConsumerStream(dataConsumer, KafkaMessage)

    //Shutdown
    shutdownApplication()

    def startAkkaHTTPServer() = {
        val settings = Settings(system).Http
        val host = settings.host

        println(s"Specify the TCP port do you want to host the HTTP server at (e.g. 8001, 8080..etc)? \nHit Return when finished:")
        val portNum = StdIn.readInt()

        println(s"Waiting for http requests at http://$host:$portNum/")
        Http().bindAndHandle(routes, host, portNum)
    }

    def shutdownApplication() = {
        scala.sys.addShutdownHook({
            println("Terminating the Application...")
            akkaHttpServer.flatMap(_.unbind())
            system.terminate()
            Await.result(system.whenTerminated, 30 seconds)
            println("Application Terminated")
        })
    }
}



