import config.AppConfig
import configuration.ConfigService
import dao.IocRepository
import filereader.FileService
import io.getquill.context.ZioJdbc.DataSourceLayer
import json.JsonConverter
import ternarytree.TernaryService
import zio.config.syntax.ZIOConfigNarrowOps
import zio.logging.Logging
import zio.{ExitCode, Has, URIO}

object Main extends zio.App {
  val dataSource = DataSourceLayer.fromPrefix("db")

  val logger = Logging.consoleErr()

  val layer = dataSource >>> IocRepository.live ++ (FileService.live >>> JsonConverter.live) ++ TernaryService.live >>> CheckService.live

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    CheckService.check
      .provideSomeLayer[Has[AppConfig.FileConfig] with Logging](layer)
      .provideSomeLayer(ConfigService.live.narrow(_.file) ++ logger)
      .exitCode
  }

}
