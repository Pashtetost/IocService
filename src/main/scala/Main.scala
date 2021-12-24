import configuration.BaseConfig
import dao.IocRepository
import filereader.FileService
import io.getquill.context.ZioJdbc.DataSourceLayer
import json.JsonConverter
import ternarytree.TernaryService
import zio.{ExitCode, URIO}


object Main extends zio.App {

  val config = new BaseConfig
  val dataSource = DataSourceLayer.fromConfig(config.dbConfig)

  val layer = dataSource >>> IocRepository.live ++ (FileService.live >>> JsonConverter.live) ++ TernaryService.live >>> CheckService.live

  val app = CheckService.check("example.txt")

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    app.provideSomeLayer(layer).map( events =>
      events.foreach{case (ev, num) => println(s"$ev -->\n $num")}
    ).exitCode
}
