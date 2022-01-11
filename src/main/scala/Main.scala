import config.AppConfig
import configuration.ConfigService
import dao.IocRepository
import filereader.FileService
import io.getquill.context.ZioJdbc.DataSourceLayer
import json.JsonConverter
import ternarytree.TernaryService
import ternarytree.TernaryService.TernaryService
import zio.clock.Clock
import zio.config.syntax.ZIOConfigNarrowOps
import zio.console.{Console, putStrLn}
import zio.logging.Logging
import zio.{ExitCode, Has, Schedule, URIO, ZIO}
import zio.stream._
import zio.console._
import zio.duration._

object Main extends zio.App {
  val dataSource = DataSourceLayer.fromPrefix("db")

  val logger = Logging.consoleErr()

  val layer = dataSource >>> IocRepository.live ++ (FileService.live >>> JsonConverter.live) ++ TernaryService.live >>> CheckService.live




  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
   // CheckService.check
   //   .provideSomeLayer[Has[AppConfig.FileConfig] with Logging](layer)
   //   .provideSomeLayer(ConfigService.live.narrow(_.file) ++ logger)
   //   .exitCode

    FileService.getStreamData
      .flatMap(bufferData => ZStream.fromIterator(bufferData.getLines()))
      .partitionEither(JsonConverter.decodeStr)
      .use{
        case (errorEvent, parsedEvent) =>
          val erStr = errorEvent
          .mapM(er => putStrLn( s"Error message: $er"))
          .repeat(Schedule.fixed(1.second))
            .runDrain

          val iocDataStr = ZStream
            .fromEffect(CheckService.getIocs)

          (for {
            iocData <- iocDataStr.repeat(Schedule.fixed(5.second))
            event <- parsedEvent.mapM(TernaryService.searchIocs(_, iocData))
          } yield event)
          .foreach(x => putStrLn(x.toString))
      }
      .provideSomeLayer[Console with Clock](layer ++ TernaryService.live ++ (FileService.live >>> JsonConverter.live) ++ FileService.live ++ ConfigService.live.narrow(_.file) ++ logger)
        .exitCode
  }

}
