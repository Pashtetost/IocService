package filereader
import config.AppConfig
import zio.logging._
import zio.stream._
import zio._

import java.io.{FileInputStream, IOException}
import scala.io.{BufferedSource, Source}


object FileService {
  type FileEnvorminent = Has[AppConfig.FileConfig] with Logging
  type FileService = Has[Service]

  val s = ZStream.fromInputStream(new FileInputStream("sdq"))

  trait Service{
    def getData: RIO[FileEnvorminent, List[String]]
    def getStreamData: ZStream[FileEnvorminent, Throwable, BufferedSource]
  }

  class Impl extends Service {
    override def getData:RIO[FileEnvorminent, List[String]] = {
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

    override def getStreamData: ZStream[FileEnvorminent, Throwable, BufferedSource] =
      ZStream.environment[Has[AppConfig.FileConfig] with Logging].flatMap(config =>
        ZStream.bracket(
          log.info(s"Starting read stream from file with path: ${config.get.path}") *>
          ZIO.effect(Source.fromFile(config.get.path)))
        (res => URIO(res.close()
        ))
      )
  }

  val live: ULayer[FileService] = ZLayer.succeed(new Impl)

  def getData: RIO[FileService with FileEnvorminent, List[String]] = ZIO.accessM(_.get.getData)

  def getStreamData: ZStream[FileService with FileEnvorminent, Throwable, BufferedSource] = ZStream.environment[FileService with FileEnvorminent].flatMap(_.get.getStreamData)
}
