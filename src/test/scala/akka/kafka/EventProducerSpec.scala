package akka.kafka

import java.util.Date

import akka.Done
import akka.actor.ActorSystem
import akka.serialization.Serialization
import akka.stream.QueueOfferResult
import akka.stream.QueueOfferResult.Enqueued
import akka.stream.scaladsl.SourceQueueWithComplete
import akka.testkit.{DefaultTimeout, EventFilter, ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.omearac.producers.EventProducer
import com.omearac.shared.AkkaStreams
import com.omearac.shared.EventMessages.{ActivatedProducerStream, MessagesPublished}
import com.omearac.shared.KafkaMessages.ExampleAppEvent
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Future


class EventProducerSpec extends TestKit(ActorSystem("EventProducerSpec",ConfigFactory.parseString("""
    akka.loggers = ["akka.testkit.TestEventListener"] """)))
    with DefaultTimeout with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll
    with AkkaStreams {

    val testProducer = TestActorRef(new EventProducer)
    val producerActor = testProducer.underlyingActor
    val mockProducerStream: SourceQueueWithComplete[Any] = new SourceQueueWithComplete[Any] {
        override def complete(): Unit = println("complete")

        override def fail(ex: Throwable): Unit = println("fail")

        override def offer(elem: Any): Future[QueueOfferResult] = Future{Enqueued}

        override def watchCompletion(): Future[Done] = Future{Done}
    }

    override def afterAll: Unit = {
        shutdown()
    }

    //Create an test event listener for the local message bus
    val testEventListener = TestProbe()
    system.eventStream.subscribe(testEventListener.ref, classOf[ExampleAppEvent])


    "Sending ActivatedProducerStream to EventProducer in receive state" should {
        "save the stream ref and change state to producing " in {
            testProducer ! ActivatedProducerStream(mockProducerStream, "TestTopic")
            Thread.sleep(500)
            producerActor.producerStream should be(mockProducerStream)
            EventFilter.error(message = "EventProducer got the unknown message while producing: testMessage", occurrences = 1) intercept {
                testProducer ! "testMessage"
            }
        }
    }

    "Sending ExampleAppEvent to system bus while EventProducer is in publishEvent state" should {
        "offer the ExampleAppEvent to the stream " in {
            val producingState = producerActor.publishEvent
            producerActor.context.become(producingState)
            producerActor.producerStream = mockProducerStream
            val dateFormat = new java.text.SimpleDateFormat("dd:MM:yy:HH:mm:ss.SSS")
            lazy val timetag = dateFormat.format(new Date(System.currentTimeMillis()))
            val eventMsg = MessagesPublished(5)
            val testMessage = ExampleAppEvent(timetag,Serialization.serializedActorPath(self),eventMsg.toString)
            system.eventStream.publish(testMessage)
            testEventListener.expectMsgPF(){
                case ExampleAppEvent(_,_,m) => if (m == eventMsg.toString) () else fail()
            }
        }
    }
 }
