package com.omearac.shared

import akka.util.Timeout
import com.omearac.shared.EventMessages.FailedMessageConversion
import com.omearac.shared.KafkaMessages.{ExampleAppEvent, KafkaMessage}
import play.api.libs.json.Json
import spray.json._

import scala.concurrent.duration._


/**
  * Here we define a typeclass which converts case class messages to/from JSON.
  * Currently, we can convert KafkaMessage and ExampleAppEvent messages to/from JSON.
  * Any additional case class types need to have conversion methods defined here.
  */


object JsonMessageConversion {
    implicit val resolveTimeout = Timeout(3 seconds)

    trait Conversion[T] {
        def convertFromJson(msg: String): Either[FailedMessageConversion, T]
        def convertToJson(msg: T): String
    }

    //Here is where we create implicit objects for each Message Type you wish to convert to/from JSON
    object Conversion extends DefaultJsonProtocol {

        implicit object KafkaMessageConversions extends Conversion[KafkaMessage]  {
            implicit val json3 = jsonFormat3(KafkaMessage)

            /**
              * Converts the JSON string from the CommittableMessage to KafkaMessage case class
              * @param msg is the json string to be converted to KafkaMessage case class
              * @return either a KafkaMessage or Unit (if conversion fails)
              */
            def convertFromJson(msg: String): Either[FailedMessageConversion, KafkaMessage] = {
                try {
                    Right(msg.parseJson.convertTo[KafkaMessage])
                }
                catch {
                    case e: Exception => Left(FailedMessageConversion("kafkaTopic", msg, "to: KafkaMessage"))
                }
            }
            def convertToJson(msg: KafkaMessage) = {
                implicit val writes = Json.writes[KafkaMessage]
                Json.toJson(msg).toString
            }
        }

        implicit object ExampleAppEventConversion extends Conversion[ExampleAppEvent] {
            implicit val json3 = jsonFormat3(ExampleAppEvent)

            /**
              * Converts the JSON string from the CommittableMessage to ExampleAppEvent case class
              * @param msg is the json string to be converted to ExampleAppEvent case class
              * @return either a ExampleAppEvent or Unit (if conversion fails)
              */
            def convertFromJson(msg: String): Either[FailedMessageConversion, ExampleAppEvent] = {
                try {
                     Right(msg.parseJson.convertTo[ExampleAppEvent])
                }
                catch {
                    case e: Exception => Left(FailedMessageConversion("kafkaTopic", msg, "to: ExampleAppEvent"))
                }
            }
            def convertToJson(msg: ExampleAppEvent) = {
                implicit val writes = Json.writes[ExampleAppEvent]
                Json.toJson(msg).toString
            }
        }

        //Adding some sweet sweet syntactic sugar
        def apply[T: Conversion] : Conversion[T] = implicitly
    }
}


