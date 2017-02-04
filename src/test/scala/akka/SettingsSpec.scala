package akka

import akka.actor.ActorSystem
import com.omearac.settings.Settings
import org.scalatest._


class SettingsSpec extends WordSpecLike with Matchers {
    val system = ActorSystem("SettingsSpec")
    val settings = Settings(system)

    "Consumer Settings" must {
        "read in correct values from config" in {
            settings.Http.host should ===("0.0.0.0")
            settings.KafkaConsumers.numberOfConsumers should ===(2)
            val consumerSettings = settings.KafkaConsumers.KafkaConsumerInfo

            val consumerA = consumerSettings("KafkaMessage")
            consumerA("bootstrap-servers") should ===("localhost:9092")
            consumerA("subscription-topic") should ===("TempChannel1")
            consumerA("groupId") should ===("group1")

            val consumerB = consumerSettings("ExampleAppEvent")
            consumerB("bootstrap-servers") should ===("localhost:9092")
            consumerB("subscription-topic") should ===("TempChannel2")
            consumerB("groupId") should ===("group2")
        }
    }

    "Producer Settings" must {
        "read in correct values from config" in {
            settings.KafkaProducers.numberOfProducers should ===(2)
            val producerSettings = settings.KafkaProducers.KafkaProducerInfo

            val producerA = producerSettings("KafkaMessage")
            producerA("bootstrap-servers") should ===("localhost:9092")
            producerA("publish-topic") should ===("TempChannel1")
            producerA("num.partitions") should ===("5")

            val producerB = producerSettings("ExampleAppEvent")
            producerB("bootstrap-servers") should ===("localhost:9092")
            producerB("publish-topic") should ===("TempChannel2")
            producerB("num.partitions") should ===("5")
        }
    }
}
