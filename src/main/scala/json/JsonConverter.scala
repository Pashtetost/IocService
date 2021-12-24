package json


import filereader.FileService
import model._
import zio._
import zio.json.{DecoderOps, DeriveJsonDecoder, JsonDecoder}


object JsonConverter {
  type JsonConverter = Has[Service]

  trait Service{
    def parse(path: String): Task[List[Event]]
  }

  implicit val decoderSubject: JsonDecoder[Subject] = DeriveJsonDecoder.gen[Subject]
  implicit val decoderObject: JsonDecoder[Object] = DeriveJsonDecoder.gen[Object]
  implicit val decoderNetwork: JsonDecoder[NetworkAsset] = DeriveJsonDecoder.gen[NetworkAsset]
  implicit val decoderEvent: JsonDecoder[Event] = DeriveJsonDecoder.gen[Event]

  class Impl(fileService: FileService.Service) extends Service {
    override def parse(path: String): Task[List[Event]] =
      for {
       rawEvents <- fileService.getData(path)
       anw <- ZIO.collectPar(rawEvents){rawEvent =>
          ZIO.fromOption(rawEvent.fromJson[Event].fold(_ => None, event => Some(event)))
        }
      } yield anw
  }

  val live: URLayer[Has[FileService.Service], Has[Service]] =
    ZLayer.fromService[FileService.Service,JsonConverter.Service](fileService => new Impl(fileService))

  def parse(path: String): RIO[JsonConverter,List[Event]] = ZIO.accessM(_.get.parse(path))
}
