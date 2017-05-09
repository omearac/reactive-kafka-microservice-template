package com.omearac.http.routes

import akka.actor.ActorRef
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.omearac.producers.DataProducer.PublishMessages
import com.omearac.shared.EventMessages.MessagesPublished

import scala.concurrent.duration._


/**
  * This trait defines the HTTP API for telling the DataProducer to publish data messages to Kafka via the Stream
  */

trait ProducerCommands {
    def log: LoggingAdapter
    def dataProducer: ActorRef

    val producerHttpCommands: Route = pathPrefix("data_producer"){
        implicit val timeout = Timeout(10 seconds)
        path("produce" / IntNumber) {
            {numOfMessagesToProduce =>
                get {
                    onSuccess(dataProducer ? PublishMessages(numOfMessagesToProduce)) {
                        case MessagesPublished(numberOfMessages) => complete(StatusCodes.OK,  numberOfMessages + " messages Produced as Ordered, Boss!")
                        case _ => complete(StatusCodes.InternalServerError)
                    }
                }
            }
        }
    }
}
