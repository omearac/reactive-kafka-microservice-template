package com.omearac.consumers

import akka.actor.{ActorRef, ActorSystem}
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerMessage, ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{Flow, Sink}
import com.omearac.shared.EventMessages.FailedMessageConversion
import com.omearac.shared.JsonMessageConversion.Conversion
import com.omearac.shared.{AkkaStreams, EventSourcing}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

/**
  * This trait defines the functions for creating the consumer stream components as well
  * as functions for which are used in the stream Flow.
  */

trait ConsumerStream extends AkkaStreams with EventSourcing {
    implicit val system: ActorSystem
    def self: ActorRef


    def createStreamSink(consumerActorSink : ActorRef) = {
        Sink.actorRefWithAck(consumerActorSink, "STREAM_INIT", "OK", "STREAM_DONE")
    }

    def createStreamSource(consumerProperties: Map[String,String])  = {
        val kafkaMBAddress = consumerProperties("bootstrap-servers")
        val groupID = consumerProperties("groupId")
        val topicSubscription = consumerProperties("subscription-topic")
        val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
            .withBootstrapServers(kafkaMBAddress)
            .withGroupId(groupID)
            .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

        Consumer.committableSource(consumerSettings, Subscriptions.topics(topicSubscription))
    }

    def createStreamFlow[msgType: Conversion] = {
        Flow[ConsumerMessage.CommittableMessage[Array[Byte], String]]
            .map(msg => (msg.committableOffset, Conversion[msgType].convertFromJson(msg.record.value)))
            //Publish the conversion error event messages returned from the JSONConversion
            .map (tuple => publishConversionErrors[msgType](tuple))
            .filter(result => result.isRight)
            .map(test => test.right.get)
            //Group the commit offsets and correctly converted messages for more efficient Kafka commits
            .batch(max = 20, tuple => (CommittableOffsetBatch.empty.updated(tuple._1), ArrayBuffer[msgType](tuple._2)))
            {(tupleOfCommitOffsetAndMsgs, tuple) =>
            (tupleOfCommitOffsetAndMsgs._1.updated(tuple._1), tupleOfCommitOffsetAndMsgs._2 :+ tuple._2)
            }
            //Take the first element of the tuple (set of commit numbers) to add to kafka commit log and then return the collection of grouped case class messages
            .mapAsync(4)(tupleOfCommitOffsetAndMsgs => commitOffsetsToKafka[msgType](tupleOfCommitOffsetAndMsgs))
            .map(msgGroup => msgGroup._2)
    }

    def commitOffsetsToKafka[msgType](tupleOfCommitOffsetAndMsgs: (ConsumerMessage.CommittableOffsetBatch, ArrayBuffer[msgType])) = Future {
        (tupleOfCommitOffsetAndMsgs._1.commitScaladsl(), tupleOfCommitOffsetAndMsgs._2)
    }

    def publishConversionErrors[msgType](tupleOfCommitOffsetAndConversionResults: (ConsumerMessage.CommittableOffset, Either[FailedMessageConversion,msgType]))
    : Either[Unit,(ConsumerMessage.CommittableOffset,msgType)] = {

        if (tupleOfCommitOffsetAndConversionResults._2.isLeft) {

            //Publish a local event that there was a failure in conversion
            publishLocalEvent(tupleOfCommitOffsetAndConversionResults._2.left.get)

            //Commit the Kafka Offset to acknowledge that the message was consumed
            Left(tupleOfCommitOffsetAndConversionResults._1.commitScaladsl())
        }
        else
            Right(tupleOfCommitOffsetAndConversionResults._1,tupleOfCommitOffsetAndConversionResults._2.right.get)
    }
}
