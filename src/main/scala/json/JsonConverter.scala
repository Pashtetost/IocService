package json


import filereader.FileService
import config.AppConfig
import model._
import zio._
import zio.json.{DecoderOps, DeriveJsonDecoder, JsonDecoder}
import zio.logging.{Logging, log}


object JsonConverter {
  type JsonConverter = Has[Service]

  trait Service{
    def decodeList: RIO[Has[AppConfig.FileConfig] with Logging, List[Event]]
    def decodeStr(str: String): UIO[Either[String, Event]]
  }

  implicit val decoderSubject: JsonDecoder[Subject] = DeriveJsonDecoder.gen[Subject]
  implicit val decoderObject: JsonDecoder[Object] = DeriveJsonDecoder.gen[Object]
  implicit val decoderNetwork: JsonDecoder[NetworkAsset] = DeriveJsonDecoder.gen[NetworkAsset]
  implicit val decoderEvent: JsonDecoder[Event] = DeriveJsonDecoder.gen[Event]

  class Impl(fileService: FileService.Service) extends Service {
    override def decodeList: RIO[Has[AppConfig.FileConfig] with Logging, List[Event]] =
      for {
       rawEvents <- fileService.getData
       _ <- log.info(s"Starting convent events from Json")
       anw <- ZIO.collectPar(rawEvents){rawEvent =>
          ZIO.fromOption(rawEvent.fromJson[Event].fold(_ => None, event => Some(event)))
        }
      } yield anw

    override def decodeStr(str: String): UIO[Either[String, Event]] =
      ZIO.succeed(str.fromJson[Event])
  }

  val live: URLayer[Has[FileService.Service], Has[Service]] =
    ZLayer.fromService[FileService.Service,JsonConverter.Service](fileService => new Impl(fileService))

  def decodeList: RIO[JsonConverter with Has[AppConfig.FileConfig] with Logging,List[Event]] = ZIO.accessM(_.get.decodeList)

  def decodeStr(str: String): URIO[JsonConverter, Either[String, Event]] = ZIO.accessM(_.get.decodeStr(str))
}
