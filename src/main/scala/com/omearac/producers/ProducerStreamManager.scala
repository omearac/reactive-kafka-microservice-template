package com.omearac.producers

import akka.actor._
import com.omearac.producers.ProducerStreamManager.Messages._
import com.omearac.settings.Settings
import com.omearac.shared.EventMessages.ActivatedProducerStream
import com.omearac.shared.JsonMessageConversion.Conversion
import com.omearac.shared.KafkaMessages.{ExampleAppEvent, KafkaMessage}

/**
  * This actor is responsible for creating and terminating the publishing akka-kafka streams.
  * Upon receiving an InitializeProducerStream message with a corresponding message type
  * (KafkaMessage or ExampleAppEvent) and producer source actor reference, this manager initializes the stream,
  * sends an ActivatedProducerStream message to the source actor and finally publishes a local event to the
  * Akka Event Bus.
  */

object ProducerStreamManager {
    object Messages {
        //CommandMessage
        case class InitializeProducerStream(producerActorRef: ActorRef, msgType: Any)
    }
}

class ProducerStreamManager extends Actor with ProducerStream {
    implicit val system = context.system

    //Get Kafka Producer Config Settings
    val settings = Settings(system).KafkaProducers

    //Edit this receive method with any new Streamed message types
    def receive: Receive = {
        case InitializeProducerStream(producerActorRef, KafkaMessage) => {

            //Get producer properties
            val producerProperties = settings.KafkaProducerInfo("KafkaMessage")
            startProducerStream[KafkaMessage](producerActorRef, producerProperties)
        }
        case InitializeProducerStream(producerActorRef, ExampleAppEvent) => {

            //Get producer properties
            val producerProperties = settings.KafkaProducerInfo("ExampleAppEvent")
            startProducerStream[ExampleAppEvent](producerActorRef, producerProperties)
        }
        case other => println(s"Producer Stream Manager got unknown message: $other")
    }


    def startProducerStream[msgType: Conversion](producerActorSource: ActorRef, producerProperties: Map[String,String])={
        val streamSource = createStreamSource[msgType]
        val streamFlow = createStreamFlow[msgType](producerProperties)
        val streamSink =createStreamSink(producerProperties)
        val producerStream = streamSource.via(streamFlow).to(streamSink).run()

        //Send the completed stream reference to the actor who wants to publish to it
        val kafkaTopic = producerProperties("publish-topic")
        producerActorSource ! ActivatedProducerStream(producerStream, kafkaTopic)
        publishLocalEvent(ActivatedProducerStream(producerStream, kafkaTopic))
    }
}
