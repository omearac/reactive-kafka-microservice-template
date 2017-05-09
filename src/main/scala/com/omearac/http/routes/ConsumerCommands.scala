package com.omearac.http.routes

import akka.actor.ActorRef
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.util.Timeout
import com.omearac.consumers.DataConsumer.{ConsumerActorReply, ManuallyInitializeStream, ManuallyTerminateStream}

import scala.concurrent.duration._

/**
  * This trait defines the HTTP API for starting and stopping the Data and Event Consumer Streams
  */

trait ConsumerCommands {
  def dataConsumer: ActorRef

  def eventConsumer: ActorRef

  def log: LoggingAdapter

  val dataConsumerHttpCommands: Route = pathPrefix("data_consumer") {
    implicit val timeout = Timeout(10 seconds)
    path("stop") {
      get {
        onSuccess(dataConsumer ? ManuallyTerminateStream) {
          case m: ConsumerActorReply => log.info(m.message); complete(StatusCodes.OK, m.message);
          case _ => complete(StatusCodes.InternalServerError)
        }
      }
    } ~
      path("start") {
        get {
          onSuccess(dataConsumer ? ManuallyInitializeStream) {
            case m: ConsumerActorReply => log.info(m.message); complete(StatusCodes.OK, m.message)
            case _ => complete(StatusCodes.InternalServerError)
          }
        }
      }
  }

  val eventConsumerHttpCommands: Route = pathPrefix("event_consumer") {
    implicit val timeout = Timeout(10 seconds)
    path("stop") {
      get {
        onSuccess(eventConsumer ? ManuallyTerminateStream) {
          case m: ConsumerActorReply => log.info(m.message); complete(StatusCodes.OK, m.message);
          case _ => complete(StatusCodes.InternalServerError)
        }
      }
    } ~
      path("start") {
        get {
          onSuccess(eventConsumer ? ManuallyInitializeStream) {
            case m: ConsumerActorReply => log.info(m.message); complete(StatusCodes.OK, m.message)
            case _ => complete(StatusCodes.InternalServerError)
          }
        }
      }
  }

}
