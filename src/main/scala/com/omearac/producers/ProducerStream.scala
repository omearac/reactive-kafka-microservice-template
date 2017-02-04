package com.omearac.producers

import akka.actor.{ActorRef, ActorSystem}
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Source}
import com.omearac.shared.JsonMessageConversion.Conversion
import com.omearac.shared.{AkkaStreams, EventSourcing}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

/**
  * This trait defines the functions for creating the producer stream components.
  */

trait ProducerStream extends AkkaStreams with EventSourcing {
    implicit val system: ActorSystem
    def self: ActorRef


    def createStreamSource[msgType] = {
        Source.queue[msgType](Int.MaxValue,OverflowStrategy.backpressure)
    }


    def createStreamSink(producerProperties: Map[String, String]) = {
        val kafkaMBAddress = producerProperties("bootstrap-servers")
        val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer).withBootstrapServers(kafkaMBAddress)

        Producer.plainSink(producerSettings)
    }


    def createStreamFlow[msgType: Conversion](producerProperties: Map[String, String]) = {
        val numberOfPartitions = producerProperties("num.partitions").toInt -1
        val topicToPublish = producerProperties("publish-topic")
        val rand = new scala.util.Random
        val range = 0 to numberOfPartitions

        Flow[msgType].map { msg =>
            val partition = range(rand.nextInt(range.length))
            val stringJSONMessage = Conversion[msgType].convertToJson(msg)
            new ProducerRecord[Array[Byte], String](topicToPublish, partition, null, stringJSONMessage)
        }
    }
}
