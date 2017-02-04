package com.omearac.producers

import akka.actor._
import akka.event.Logging
import akka.stream.scaladsl.SourceQueueWithComplete
import com.omearac.producers.DataProducer.Messages.PublishMessages
import com.omearac.shared.EventMessages.{ActivatedProducerStream, MessagesPublished}
import com.omearac.shared.EventSourcing
import com.omearac.shared.KafkaMessages.KafkaMessage

/**
  * This actor publishes 'KafkaMessage's to the Kafka topic TestDataChannel. The idea would be that another microservice is subscribed to
  * the TestDataChannel topic and can then react to data messages this microservice emits.
  * This actor gets the stream connection reference from the ProducerStreamManager such that when he gets a
  * PublishMessages command message from the HTTP interface, he will create KafkaMessages and then offer/send them to the stream.
  */

object DataProducer {
    object Messages {
        //Command Messages
        case class PublishMessages(numberOfMessages: Int)
    }
}

class DataProducer extends Actor with EventSourcing {
    import context._
    implicit val system = context.system
    val log = Logging(system, this.getClass.getName)

    var producerStream: SourceQueueWithComplete[Any] = null

    def receive: Receive = {
        case ActivatedProducerStream(streamRef, kafkaTopic) =>
            producerStream = streamRef
            become(publishData)
        case msg: PublishMessages => if (producerStream == null) self ! msg
        case other => log.error("DataProducer got the unknown message while in idle: " + other)
    }

    def publishData: Receive = {
        case PublishMessages(numberOfMessages) =>
            for ( i <- 1 to numberOfMessages) {
                val myPublishableMessage = KafkaMessage(timetag, " send me to kafka, yo!", i)
                producerStream.offer(myPublishableMessage)
            }

            //Tell the akka-http front end that messages were sent
            sender() ! MessagesPublished(numberOfMessages)
            publishLocalEvent(MessagesPublished(numberOfMessages))
        case other => log.error("DataProducer got the unknown message while producing: " + other)
    }
}
