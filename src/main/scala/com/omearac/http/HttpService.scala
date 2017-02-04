package com.omearac.http

import akka.http.scaladsl.server.Directives._
import com.omearac.http.routes.{ConsumerCommands, ProducerCommands}


trait HttpService extends ConsumerCommands with ProducerCommands {
    //Joining the Http Routes
    def routes = producerHttpCommands ~ dataConsumerHttpCommands ~ eventConsumerHttpCommands
}
