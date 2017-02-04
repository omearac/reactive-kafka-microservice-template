package com.omearac.shared

import akka.stream.scaladsl.SourceQueueWithComplete

/**
  * EventMessages are those which are emitted throughout the application and KafkaMessages are those which
  * are converted to/from JSON to be published/consumed to/from Kafka.
  * The EventMessages are converted to ExampleAppEvents when they are published.
  */

object EventMessages {
    abstract class EventMessage
    case class ActivatedConsumerStream(kafkaTopic: String) extends EventMessage
    case class TerminatedConsumerStream(kafkaTopic: String) extends EventMessage
    case class ActivatedProducerStream[msgType](producerStream: SourceQueueWithComplete[msgType], kafkaTopic: String) extends EventMessage
    case class MessagesPublished(numberOfMessages: Int) extends EventMessage
    case class FailedMessageConversion(kafkaTopic: String, msg: String, msgType: String) extends EventMessage
}

object KafkaMessages {
    case class KafkaMessage(time: String, subject: String, item: Int)
    case class ExampleAppEvent(time: String, senderID: String, eventType: String)
}