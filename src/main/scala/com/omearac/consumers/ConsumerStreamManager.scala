package com.omearac.consumers

import akka.actor._
import akka.event.Logging
import akka.kafka.scaladsl.Consumer.Control
import com.omearac.consumers.ConsumerStreamManager.Messages.{InitializeConsumerStream, TerminateConsumerStream}
import com.omearac.settings.Settings
import com.omearac.shared.EventMessages.{ActivatedConsumerStream, TerminatedConsumerStream}
import com.omearac.shared.JsonMessageConversion.Conversion
import com.omearac.shared.KafkaMessages.{ExampleAppEvent, KafkaMessage}

import scala.collection.mutable


/**
  * This actor is responsible for creating and terminating the consuming akka-kafka streams.
  * Upon receiving an InitializeConsumerStream message with a corresponding message type
  * (KafkaMessage or ExampleAppEvent) and consumer sink actor reference, this manager initializes the stream,
  * sends an ActivatedConsumerStream message to the sink actor and finally publishes a local event to the
  * Akka Event Bus.
  *
  * When this actor receives a TerminateConsumerStream message along with an associated kafka topic, he gets
  * the corresponding stream reference from its activeConsumerStreams collection and then shuts down the stream.
  */

object ConsumerStreamManager {
    object Messages {
        //Command Messages
        case class InitializeConsumerStream(consumerActorRef: ActorRef, msgType: Any)
        case class TerminateConsumerStream(kafkaTopic: String)
    }
}

class ConsumerStreamManager extends Actor with ConsumerStream {
    implicit val system = context.system
    val log = Logging(system, this.getClass.getName)

    //Once the stream is created, we store its reference and associated kafka topic so we can shut it down on command
    var activeConsumerStreams: mutable.Map[String,Control] = mutable.Map()

    //Get Kafka Consumer Config Settings
    val settings = Settings(system).KafkaConsumers

    def receive: Receive = {
        case InitializeConsumerStream(consumerActorRef,KafkaMessage) =>

            //Get consumer properties corresponding to that which subscribes to message type KafkaMessage
            val consumerProperties = settings.KafkaConsumerInfo("KafkaMessage")
            startConsumerStream[KafkaMessage](consumerActorRef, consumerProperties)

        case InitializeConsumerStream(consumerActorRef,ExampleAppEvent) =>

            //Get consumer properties corresponding to that which subscribes to the message type ExampleAppEvent
            val consumerProperties = settings.KafkaConsumerInfo("ExampleAppEvent")
            startConsumerStream[ExampleAppEvent](consumerActorRef, consumerProperties)

        case TerminateConsumerStream(kafkaTopic) => terminateConsumerStream(sender, kafkaTopic)

        case other => log.error(s"Consumer Stream Manager got unknown message: $other")
    }


    def startConsumerStream[msgType: Conversion](consumerActorSink: ActorRef, consumerProperties: Map[String,String]) = {
        val streamSource = createStreamSource(consumerProperties)
        val streamFlow = createStreamFlow[msgType]
        val streamSink = createStreamSink(consumerActorSink)
        val consumerStream = streamSource.via(streamFlow).to(streamSink).run()

        //Add the active consumer stream reference and topic to the active stream collection
        val kafkaTopic = consumerProperties("subscription-topic")
        activeConsumerStreams+= kafkaTopic -> consumerStream

        //Tell the consumer actor sink the stream has been started for the kafka topic and publish the event
        consumerActorSink ! ActivatedConsumerStream(kafkaTopic)
        publishLocalEvent(ActivatedConsumerStream(kafkaTopic))
    }


    def terminateConsumerStream(consumerActorSink: ActorRef, kafkaTopic: String) = {
        try {
            println(s"ConsumerStreamManager got TerminateStream command for topic: $kafkaTopic. Terminating stream...")
            val stream = activeConsumerStreams(kafkaTopic)
            val stopped = stream.stop

            stopped.onComplete {
                case _ =>
                    stream.shutdown()

                    //Remove the topic name from activeConsumerStreams collection
                    activeConsumerStreams-= kafkaTopic

                    //Publish an app event that the stream was killed. The stream will send an onComplete message to the Sink
                    publishLocalEvent(TerminatedConsumerStream(kafkaTopic))
                    println(s"Terminated stream for topic: $kafkaTopic.")
            }
        }
        catch {
            case e: NoSuchElementException =>
                consumerActorSink ! "STREAM_DONE"
                log.info(s"Stream Consumer in consuming mode but no stream to consume from: ($consumerActorSink,$kafkaTopic)")
            case e: Exception => log.error(s"Exception during manual termination of the Consumer Stream for topic $kafkaTopic : $e")
        }
    }
}
