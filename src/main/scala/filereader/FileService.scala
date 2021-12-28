package filereader
import config.AppConfig
import zio.logging._
import zio._

import scala.io.Source


object FileService {
  type FileService = Has[Service]

  trait Service{
    def getData: RIO[Has[AppConfig.FileConfig] with Logging, List[String]]
  }

  class Impl extends Service {
    override def getData:RIO[Has[AppConfig.FileConfig] with Logging, List[String]] = {
      ZIO.accessM[Has[AppConfig.FileConfig] with Logging](config =>
        ZIO.bracket(
          log.info(s"Starting read file with path: ${config.get.path}") *>
            ZIO.effect(Source.fromFile(config.get.path)
          ))
        (
          res => URIO(res.close())
        )(
          file => ZIO.effect(file.getLines().toList)
        )
      )
    }
  }

  val live: ULayer[FileService] = ZLayer.succeed(new Impl)

  def getData: RIO[FileService with Has[AppConfig.FileConfig] with Logging, List[String]] = ZIO.accessM(_.get.getData)
}
