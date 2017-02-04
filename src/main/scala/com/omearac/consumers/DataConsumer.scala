package com.omearac.consumers

import akka.actor._
import akka.event.Logging
import com.omearac.consumers.ConsumerStreamManager.Messages.{InitializeConsumerStream, TerminateConsumerStream}
import com.omearac.consumers.DataConsumer.Messages._
import com.omearac.settings.Settings
import com.omearac.shared.EventMessages.ActivatedConsumerStream
import com.omearac.shared.EventSourcing
import com.omearac.shared.KafkaMessages.KafkaMessage

import scala.collection.mutable.ArrayBuffer


/**
  * This actor serves as a Sink for the kafka stream that is created by the ConsumerStreamManager.
  * The corresponding stream converts the json from the kafka topic TestDataChannel to the message type KafkaMessage.
  * Once this actor receives a batch of such messages he prints them out.
  *
  * This actor can be started and stopped manually from the HTTP interface, and in doing so, changes between receiving
  * states.
  */

object DataConsumer {
    object Messages {
        //Command Messages
        case object ManuallyInitializeStream
        case object ManuallyTerminateStream

        //Document Messages
        case class ConsumerActorReply(message: String)
    }
}

class DataConsumer extends Actor with EventSourcing {
    implicit val system = context.system
    val log = Logging(system, this.getClass.getName)

    //Once stream is started by manager, we save the actor ref of the manager
    var consumerStreamManager: ActorRef = null

    //Get Kafka Topic
    val kafkaTopic = Settings(system).KafkaConsumers.KafkaConsumerInfo("KafkaMessage")("subscription-topic")

    def receive: Receive = {

        case ActivatedConsumerStream(_) => consumerStreamManager = sender()

        case "STREAM_INIT" =>
            sender() ! "OK"
            println("Data Consumer entered consuming state!")
            context.become(consumingData)

        case ManuallyTerminateStream => sender() ! ConsumerActorReply("Data Consumer Stream Already Stopped")

        case ManuallyInitializeStream =>
            consumerStreamManager ! InitializeConsumerStream(self, KafkaMessage)
            sender() ! ConsumerActorReply("Data Consumer Stream Started")

        case other => log.error("Data Consumer got unknown message while in idle:" + other )
    }

    def consumingData: Receive = {
        case ActivatedConsumerStream(_) => consumerStreamManager = sender()

        case consumerMessageBatch: ArrayBuffer[_] =>
            sender() ! "OK"
            consumerMessageBatch.foreach(println)

        case "STREAM_DONE" =>
            context.become(receive)

        case ManuallyInitializeStream => sender() ! ConsumerActorReply("Data Consumer Already Started")

        case ManuallyTerminateStream =>
            consumerStreamManager ! TerminateConsumerStream(kafkaTopic)
            sender() ! ConsumerActorReply("Data Consumer Stream Stopped")

        case other => log.error("Data Consumer got Unknown message while in consuming " + other)
    }
}
