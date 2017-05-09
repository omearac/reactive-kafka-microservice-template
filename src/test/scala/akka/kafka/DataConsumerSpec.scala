package akka.kafka

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import com.omearac.consumers.ConsumerStreamManager.{InitializeConsumerStream, TerminateConsumerStream}
import com.omearac.consumers.DataConsumer
import com.omearac.consumers.DataConsumer.{ConsumerActorReply, ManuallyInitializeStream, ManuallyTerminateStream}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.collection.mutable.ArrayBuffer


class DataConsumerSpec extends TestKit(ActorSystem("DataConsumerSpec"))
  with DefaultTimeout with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  //Creating the Actors
  val testConsumer = TestActorRef(new DataConsumer)
  val mockStreamAndManager = system.actorOf(Props(new MockStreamAndManager), "mockStreamAndManager")

  override def afterAll: Unit = {
    shutdown()
  }

  class MockStreamAndManager extends Actor {
    val receive: Receive = {
      case InitializeConsumerStream(_, _) => testConsumer ! "STREAM_INIT"
      case TerminateConsumerStream(_) => testConsumer ! "STREAM_DONE"
    }
  }


  "Sending ManuallyTerminateStream to DataConsumer in receive state" should {
    "return a Stream Already Stopped reply " in {
      testConsumer ! ManuallyTerminateStream
      expectMsg(ConsumerActorReply("Data Consumer Stream Already Stopped"))
    }
  }

  "Sending ManuallyInitializeStream to DataConsumer in receive state" should {
    "forward the message to the ConsumerStreamManager and change state to consuming" in {
      testConsumer.underlyingActor.consumerStreamManager = mockStreamAndManager
      testConsumer ! ManuallyInitializeStream
      expectMsg(ConsumerActorReply("Data Consumer Stream Started"))
      //Now check for state change
      Thread.sleep(750)
      testConsumer ! ManuallyInitializeStream
      expectMsg(ConsumerActorReply("Data Consumer Already Started"))
    }
  }

  "Sending STREAM_DONE to DataConsumer while in consuming state" should {
    "change state to idle state" in {
      val consuming = testConsumer.underlyingActor.consumingData
      testConsumer.underlyingActor.context.become(consuming)
      testConsumer ! "STREAM_DONE"
      //Now check for state change
      Thread.sleep(750)
      testConsumer ! ManuallyTerminateStream
      expectMsg(ConsumerActorReply("Data Consumer Stream Already Stopped"))
    }
  }
  "Sending ManuallyTerminateStream to DataConsumer while in consuming state" should {
    "forward the message to the ConsumerStreamManager and then upon reply, change state to idle" in {
      val consuming = testConsumer.underlyingActor.consumingData
      testConsumer.underlyingActor.context.become(consuming)
      testConsumer ! ManuallyTerminateStream
      expectMsg(ConsumerActorReply("Data Consumer Stream Stopped"))
      //Now check for state change
      Thread.sleep(750)
      testConsumer ! ManuallyTerminateStream
      expectMsg(ConsumerActorReply("Data Consumer Stream Already Stopped"))
    }
  }

  "Sending ConsumerMessageBatch message" should {
    "reply OK" in {
      val msgBatch: ArrayBuffer[String] = ArrayBuffer("test1")
      val consuming = testConsumer.underlyingActor.consumingData
      testConsumer.underlyingActor.context.become(consuming)
      testConsumer.underlyingActor.consumerStreamManager = mockStreamAndManager
      testConsumer ! msgBatch
      expectMsg("OK")
    }
  }
}
