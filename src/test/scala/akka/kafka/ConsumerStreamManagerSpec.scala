package akka.kafka

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.omearac.consumers.ConsumerStreamManager
import com.omearac.consumers.ConsumerStreamManager.{InitializeConsumerStream, TerminateConsumerStream}
import com.omearac.shared.AkkaStreams
import com.omearac.shared.EventMessages.{ActivatedConsumerStream, TerminatedConsumerStream}
import com.omearac.shared.KafkaMessages.{ExampleAppEvent, KafkaMessage}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.collection.mutable.ArrayBuffer


class ConsumerStreamManagerSpec extends TestKit(ActorSystem("ConsumerStreamManagerSpec"))
  with DefaultTimeout with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll
  with AkkaStreams {

  val testConsumerStreamManager = TestActorRef(new ConsumerStreamManager)
  val consumerStreamManagerActor = testConsumerStreamManager.underlyingActor

  //Create an test event listener for the local message bus
  val testEventListener = TestProbe()
  system.eventStream.subscribe(testEventListener.ref, classOf[ExampleAppEvent])


  override def afterAll: Unit = {
    shutdown()
  }


  "Sending InitializeConsumerStream(self, KafkaMessage) to ConsumerStreamManager" should {
    "initialize the stream for that particular message type, return ActivatedConsumerStream(\"TempChannel1\") and produce local event " in {
      testConsumerStreamManager ! InitializeConsumerStream(self, KafkaMessage)
      val eventMsg = ActivatedConsumerStream("TempChannel1")
      val expectedMsgs = Seq(eventMsg, "STREAM_INIT")
      var receivedMsgs = ArrayBuffer[Any]()

      while (receivedMsgs.length < expectedMsgs.length) {
        expectMsgPF() {
          case msg: ActivatedConsumerStream => receivedMsgs += msg
          case "STREAM_INIT" => receivedMsgs += "STREAM_INIT"
        }
      }
      testEventListener.expectMsgPF() {
        case ExampleAppEvent(_, _, m) => if (m == eventMsg.toString) () else fail()
      }
    }
  }

  "Sending InitializeConsumerStream(self, ExampleAppEvent) to ConsumerStreamManager" should {
    "initialize the stream for that particular message type, return ActivatedConsumerStream(\"TempChannel2\") and produce local event " in {
      testConsumerStreamManager ! InitializeConsumerStream(self, ExampleAppEvent)
      val eventMsg = ActivatedConsumerStream("TempChannel2")
      val expectedMsgs = Seq(eventMsg, "STREAM_INIT")
      var receivedMsgs = ArrayBuffer[Any]()

      while (receivedMsgs.length < expectedMsgs.length) {
        expectMsgPF() {
          case msg: ActivatedConsumerStream => receivedMsgs += msg
          case "STREAM_INIT" => receivedMsgs += "STREAM_INIT"
        }
      }
      testEventListener.expectMsgPF() {
        case ExampleAppEvent(_, _, m) => if (m == eventMsg.toString) () else fail()
      }
    }
  }

  "Sending TerminateConsumerStream(\"TempChannel1\") to ConsumerStreamManager" should {
    "terminate the stream for that particular message type associated to the channel, " +
      "the Stream will return a \"STREAM_DONE\" msg and then a TerminatedConsumerStream(\"TempChannel1\") event will be and produced locally " in {
      //First resetting the internal state of the manager that keeps the active channels during testing
      consumerStreamManagerActor.activeConsumerStreams -= "TempChannel1"
      consumerStreamManagerActor.activeConsumerStreams -= "TempChannel2"

      testConsumerStreamManager ! InitializeConsumerStream(self, KafkaMessage)
      val eventMsg = ActivatedConsumerStream("TempChannel1")
      val expectedMsgs = Seq(eventMsg, "STREAM_INIT")
      var receivedMsgs = ArrayBuffer[Any]()

      while (receivedMsgs.length < expectedMsgs.length) {
        expectMsgPF() {
          case msg: ActivatedConsumerStream => receivedMsgs += msg
          case "STREAM_INIT" => receivedMsgs += "STREAM_INIT"
        }
      }
      consumerStreamManagerActor.activeConsumerStreams.size shouldBe 1
      testEventListener.expectMsgPF() {
        case ExampleAppEvent(_, _, m) => if (m == eventMsg.toString) () else fail()
      }

      testConsumerStreamManager ! TerminateConsumerStream("TempChannel1")
      while (receivedMsgs.length < expectedMsgs.length + 1) {
        expectMsgPF() {
          case "STREAM_DONE" => receivedMsgs += "STREAM_DONE"
          case "STREAM_INIT" => () //do nothing since this is left over from the previous message test
        }
      }
      Thread.sleep(500)
      consumerStreamManagerActor.activeConsumerStreams.size shouldBe 0
      val resultMessage2 = TerminatedConsumerStream("TempChannel1")
      testEventListener.expectMsgPF() {
        case ExampleAppEvent(_, _, m) => if (m == resultMessage2.toString) () else fail()
      }
    }
  }
}
