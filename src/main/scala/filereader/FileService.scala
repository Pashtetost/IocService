package filereader
import config.AppConfig
import zio.logging._
import zio.stream._
import zio._

import scala.io.{BufferedSource, Source}


object FileService {
  type FileService = Has[Service]


  trait Service{
    def getData: Task[List[String]]
    def getStreamData: ZStream[Any, Throwable, BufferedSource]
  }

  val live: URLayer[Has[AppConfig] with Logging, FileService] = ZLayer.fromFunction(env => new Service {
    override def getData: Task[List[String]] = ZIO.bracket(
      log.info(s"Starting read file with path: ${env.get.file.path}") *>
        ZIO.effect(Source.fromFile(env.get.file.path))
    )(res =>
      URIO(res.close())
    )(file => ZIO.effect(file.getLines()).map(_.toList))
      .provide(env)

    override def getStreamData: ZStream[Any, Throwable, BufferedSource] =  ZStream.bracket(
      log.info(s"Starting read stream from file with path: ${env.get.file.path}") *>
        ZIO.effect(Source.fromFile(env.get.file.path)))(res => URIO(res.close()))
      .provide(env)
  })

  def getData: RIO[FileService, List[String]] = ZIO.accessM(_.get.getData)

  def getStreamData: ZStream[FileService, Throwable, BufferedSource] = ZStream.accessStream(_.get.getStreamData)
}
