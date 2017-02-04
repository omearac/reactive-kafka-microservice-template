package com.omearac.shared

import java.util.Date

import akka.actor.{ActorRef, ActorSystem}
import akka.serialization._
import com.omearac.shared.EventMessages.EventMessage
import com.omearac.shared.KafkaMessages.ExampleAppEvent

/**
  * This trait converts EventMessages to ExampleAppEvents and defines the method for publishing them to the local
  * Akka Event Bus. The conversion occurs since we eventally publish the ExampleAppEvents to Kafka via a stream once
  * they're picked up from the local bus.
  */

trait EventSourcing {
    implicit val system: ActorSystem
    val dateFormat = new java.text.SimpleDateFormat("dd:MM:yy:HH:mm:ss.SSS")
    lazy val timetag = dateFormat.format(new Date(System.currentTimeMillis()))
    def self: ActorRef


    def publishLocalEvent(msg: EventMessage) : Unit = {
        val exampleAppEvent = ExampleAppEvent(timetag, Serialization.serializedActorPath(self), msg.toString)
        system.eventStream.publish(exampleAppEvent)
    }
}


