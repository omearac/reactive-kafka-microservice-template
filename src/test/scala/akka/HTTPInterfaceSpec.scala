package akka

import akka.event.Logging
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.QueueOfferResult
import akka.stream.QueueOfferResult.Enqueued
import akka.stream.scaladsl.SourceQueueWithComplete
import akka.testkit.{TestActorRef, TestProbe}
import com.omearac.consumers.{DataConsumer, EventConsumer}
import com.omearac.http.routes.{ConsumerCommands, ProducerCommands}
import com.omearac.producers.DataProducer
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future


class HTTPInterfaceSpec extends WordSpec
    with Matchers with ScalatestRouteTest
    with ConsumerCommands with ProducerCommands {

    val log = Logging(system, this.getClass.getName)

    //Mocks for DataConsumer Tests
    val dataConsumer = TestActorRef(new DataConsumer)
    val manager = TestProbe()
    dataConsumer.underlyingActor.consumerStreamManager = manager.ref

    //Mocks for EventConsumer Tests
    val eventConsumer = TestActorRef(new EventConsumer)
    eventConsumer.underlyingActor.consumerStreamManager = manager.ref

    //Mocks for DataProducer Tests
    val dataProducer = TestActorRef(new DataProducer)
    val mockProducerStream: SourceQueueWithComplete[Any] = new SourceQueueWithComplete[Any] {
        override def complete(): Unit = println("complete")

        override def fail(ex: Throwable): Unit = println("fail")

        override def offer(elem: Any): Future[QueueOfferResult] = Future{Enqueued}

        override def watchCompletion(): Future[Done] = Future{Done}
    }


    "The HTTP interface to control the DataConsumerStream" should {
        "return a Already Stopped message for GET requests to /data_consumer/stop" in {
            Get("/data_consumer/stop") ~> dataConsumerHttpCommands ~> check {
                responseAs[String] shouldEqual "Data Consumer Stream Already Stopped"
            }
        }

        "return a Stream Started response for GET requests to /data_consumer/start" in {
            Get("/data_consumer/start") ~> dataConsumerHttpCommands ~> check {
                responseAs[String] shouldEqual "Data Consumer Stream Started"
            }
        }
    }

    "The HTTP interface to control the EventConsumerStream" should {
        "return a Already Stopped message for GET requests to /event_consumer/stop" in {
            Get("/event_consumer/stop") ~> eventConsumerHttpCommands ~> check {
                responseAs[String] shouldEqual "Event Consumer Stream Already Stopped"
            }
        }

        "return a Stream Started response for GET requests to /data_consumer/start" in {
            Get("/event_consumer/start") ~> eventConsumerHttpCommands ~> check {
                responseAs[String] shouldEqual "Event Consumer Stream Started"
            }
        }
    }

    "The HTTP interface to tell the DataProducer Actor to publish messages to Kafka" should {
        "return a Messages Produced message for GET requests to /data_producer/produce/10" in {
            dataProducer.underlyingActor.producerStream = mockProducerStream
            val producing = dataProducer.underlyingActor.publishData
            dataProducer.underlyingActor.context.become(producing)

            Get("/data_producer/produce/10") ~> producerHttpCommands ~> check {
                responseAs[String] shouldEqual "10 messages Produced as Ordered, Boss!"
            }
        }
    }
}

