package com.omearac.shared

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

/**
  *This trait contains the components required to materialize and run Akka Streams
  */

trait AkkaStreams {
    implicit val system: ActorSystem
    implicit def executionContext = system.dispatcher
    implicit def materializer = ActorMaterializer()
}
