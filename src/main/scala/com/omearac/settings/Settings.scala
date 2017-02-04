package com.omearac.settings

import akka.actor.ActorSystem

/**
  * In this class we read in the application.conf configuration file to get the various consumer/producer settings
  * as well as the akka-http host
  */

class Settings(system:ActorSystem) {
    object Http {
        val host = system.settings.config.getString("http.host")
    }
    object KafkaProducers {
        val numberOfProducers = system.settings.config.getInt("akka.kafka.producer.num-producers")

        //TODO: We only have one bootstrap server (kafka broker) at the moment so we get one IP below)
        val KafkaProducerInfo: Map[String, Map[String,String]] = (for (i <- 1 to numberOfProducers) yield {
            val kafkaMessageType = system.settings.config.getString(s"akka.kafka.producer.p$i.message-type")
            val kafkaMessageBrokerIP = system.settings.config.getString(s"akka.kafka.producer.p$i.bootstrap-servers")
            val kafkaTopic = system.settings.config.getString(s"akka.kafka.producer.p$i.publish-topic")
            val numberOfPartitions = system.settings.config.getString(s"akka.kafka.producer.p$i.num.partitions")
            kafkaMessageType -> Map("bootstrap-servers" -> kafkaMessageBrokerIP, "publish-topic" -> kafkaTopic, "num.partitions" -> numberOfPartitions)
        }).toMap
    }

    object KafkaConsumers {
        val numberOfConsumers = system.settings.config.getInt("akka.kafka.consumer.num-consumers")

        //TODO: We only have one bootstrap server (kafka broker) at the moment so we get one IP below)
        val KafkaConsumerInfo: Map[String, Map[String,String]] = (for (i <- 1 to numberOfConsumers) yield {
            val kafkaMessageType = system.settings.config.getString(s"akka.kafka.consumer.c$i.message-type")
            val kafkaMessageBrokerIP = system.settings.config.getString(s"akka.kafka.consumer.c$i.bootstrap-servers")
            val kafkaTopic = system.settings.config.getString(s"akka.kafka.consumer.c$i.subscription-topic")
            val groupId = system.settings.config.getString(s"akka.kafka.consumer.c$i.groupId")
            kafkaMessageType -> Map("bootstrap-servers" -> kafkaMessageBrokerIP, "subscription-topic" -> kafkaTopic, "groupId" -> groupId)
        }).toMap
    }
}

object Settings {
    def apply(system: ActorSystem) = new Settings(system)
}