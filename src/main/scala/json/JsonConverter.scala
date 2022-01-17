package json


import model._
import zio._
import zio.json.{DecoderOps, DeriveJsonDecoder, DeriveJsonEncoder, EncoderOps, JsonDecoder, JsonEncoder}
import zio.logging.{Logging, log}


object JsonConverter {
  type JsonConverter = Has[Service]

  trait Service{
    def decodeList(listRaw: List[String]): UIO[List[Event]]
    def decodeStr(str: String): Task[Either[String, Event]]
    def encodeEvent(event: Event): UIO[String]
  }

  implicit private val decoderSubject: JsonDecoder[Subject] = DeriveJsonDecoder.gen[Subject]
  implicit private val decoderObject: JsonDecoder[Object] = DeriveJsonDecoder.gen[Object]
  implicit private val decoderNetwork: JsonDecoder[NetworkAsset] = DeriveJsonDecoder.gen[NetworkAsset]
  implicit private val decoderEvent: JsonDecoder[Event] = DeriveJsonDecoder.gen[Event]
  implicit private val encoderSubject: JsonEncoder[Subject] = DeriveJsonEncoder.gen[Subject]
  implicit private val encoderObject: JsonEncoder[Object] = DeriveJsonEncoder.gen[Object]
  implicit private val encoderNetwork: JsonEncoder[NetworkAsset] = DeriveJsonEncoder.gen[NetworkAsset]
  implicit private val encoderEvent: JsonEncoder[Event] = DeriveJsonEncoder.gen[Event]

  val live: URLayer[Logging , JsonConverter] = ZLayer.fromFunction(env => new Service {
    override def decodeList(listRaw: List[String]): UIO[List[Event]] =
        (for {
          _ <- log.info(s"Starting convent events from Json")
          anw <- ZIO.collectPar(listRaw){rawEvent =>
            ZIO.fromOption(rawEvent.fromJson[Event].fold(_ => None, event => Some(event)))
          }
        } yield anw).provide(env)

    override def decodeStr(str: String): Task[Either[String, Event]] =
        ZIO.effect(str.fromJson[Event])

    override def encodeEvent(event: Event): UIO[String] =
      ZIO.succeed(event.toJson)

  })

  def decodeList(listRaw: List[String]): URIO[JsonConverter, List[Event]] = ZIO.accessM(_.get.decodeList(listRaw))

  def decodeStr(str: String): RIO[JsonConverter, Either[String, Event]] = ZIO.accessM(_.get.decodeStr(str))

  def encodeEvent(event: Event): URIO[JsonConverter, String] = ZIO.accessM(_.get.encodeEvent(event))
}
