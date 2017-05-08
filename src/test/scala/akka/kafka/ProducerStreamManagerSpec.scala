package akka.kafka

import akka.actor.ActorSystem
import akka.stream.scaladsl.SourceQueueWithComplete
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.omearac.producers.ProducerStreamManager
import com.omearac.producers.ProducerStreamManager.InitializeProducerStream
import com.omearac.shared.AkkaStreams
import com.omearac.shared.EventMessages.ActivatedProducerStream
import com.omearac.shared.KafkaMessages.{ExampleAppEvent, KafkaMessage}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


class ProducerStreamManagerSpec extends TestKit(ActorSystem("ProducerStreamManagerSpec"))
  with DefaultTimeout with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll
  with AkkaStreams {

  val testProducerStreamManager = TestActorRef(new ProducerStreamManager)
  val producerStreamManagerActor = testProducerStreamManager.underlyingActor

  //Create an test event listener for the local message bus
  val testEventListener = TestProbe()
  system.eventStream.subscribe(testEventListener.ref, classOf[ExampleAppEvent])

  override def afterAll: Unit = {
    shutdown()
  }


  "Sending InitializeProducerStream(self, KafkaMessage) to ProducerStreamManager" should {
    "initialize the stream for that particular message type, return ActivatedProducerStream(streaRef, \"TempChannel1\") and produce local event " in {
      testProducerStreamManager ! InitializeProducerStream(self, KafkaMessage)
      Thread.sleep(500)
      var streamRef: SourceQueueWithComplete[Any] = null
      expectMsgPF() {
        case ActivatedProducerStream(sr, kt) => if (kt == "TempChannel1") {
          streamRef = sr; ()
        } else fail()
      }

      Thread.sleep(500)
      val resultMessage = ActivatedProducerStream(streamRef, "TempChannel1")
      testEventListener.expectMsgPF() {
        case ExampleAppEvent(_, _, m) => if (m == resultMessage.toString) () else fail()
      }
    }
  }

  "Sending InitializeProducerStream(self, ExampleAppEvent) to ProducerStreamManager" should {
    "initialize the stream for that particular message type, return ActivatedProducerStream(streaRef, \"TempChannel2\") and produce local event " in {
      testProducerStreamManager ! InitializeProducerStream(self, ExampleAppEvent)
      Thread.sleep(500)
      var streamRef: SourceQueueWithComplete[Any] = null
      expectMsgPF() {
        case ActivatedProducerStream(sr, kt) => if (kt == "TempChannel2") {
          streamRef = sr; ()
        } else fail()
      }

      Thread.sleep(500)
      val resultMessage = ActivatedProducerStream(streamRef, "TempChannel2")
      testEventListener.expectMsgPF() {
        case ExampleAppEvent(_, _, m) => if (m == resultMessage.toString) () else fail()
      }
    }
  }
}
