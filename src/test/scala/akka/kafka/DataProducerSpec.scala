package akka.kafka

import akka.Done
import akka.actor.ActorSystem
import akka.stream.QueueOfferResult
import akka.stream.QueueOfferResult.Enqueued
import akka.stream.scaladsl.SourceQueueWithComplete
import akka.testkit.{DefaultTimeout, EventFilter, ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.omearac.producers.DataProducer
import com.omearac.producers.DataProducer.PublishMessages
import com.omearac.shared.AkkaStreams
import com.omearac.shared.EventMessages.{ActivatedProducerStream, MessagesPublished}
import com.omearac.shared.KafkaMessages.ExampleAppEvent
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Future


class DataProducerSpec extends TestKit(ActorSystem("DataProducerSpec", ConfigFactory.parseString(
  """
    akka.loggers = ["akka.testkit.TestEventListener"] """)))
  with DefaultTimeout with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll
  with AkkaStreams {

  val testProducer = TestActorRef(new DataProducer)
  val producerActor = testProducer.underlyingActor

  val mockProducerStream: SourceQueueWithComplete[Any] = new SourceQueueWithComplete[Any] {
    override def complete(): Unit = println("complete")

    override def fail(ex: Throwable): Unit = println("fail")

    override def offer(elem: Any): Future[QueueOfferResult] = Future {
      Enqueued
    }

    override def watchCompletion(): Future[Done] = Future {
      Done
    }
  }

  override def afterAll: Unit = {
    shutdown()
  }

  //Create an test event listener for the local message bus
  val testEventListener = TestProbe()
  system.eventStream.subscribe(testEventListener.ref, classOf[ExampleAppEvent])


  "Sending ActivatedProducerStream to DataProducer in receive state" should {
    "save the stream ref and change state to producing " in {
      testProducer ! ActivatedProducerStream(mockProducerStream, "TestTopic")
      Thread.sleep(500)
      producerActor.producerStream should be(mockProducerStream)
      EventFilter.error(message = "DataProducer got the unknown message while producing: testMessage", occurrences = 1) intercept {
        testProducer ! "testMessage"
      }
    }
  }

  "Sending PublishMessages(number: Int) to DataProducer in publishData state" should {
    "return MessagesPublished(number: Int) and publish the local event " in {
      val producing = producerActor.publishData
      producerActor.context.become(producing)
      producerActor.producerStream = mockProducerStream
      val resultMessage = MessagesPublished(5)
      testProducer ! PublishMessages(5)
      expectMsg(resultMessage)
      testEventListener.expectMsgPF() {
        case ExampleAppEvent(_, _, m) => if (m == resultMessage.toString) () else fail()
      }
    }
  }
}
