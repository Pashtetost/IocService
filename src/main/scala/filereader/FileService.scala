package filereader

import zio._

import scala.io.Source


object FileService {
  type FileService = Has[Service]

  trait Service{
    def getData(path: String): Task[List[String]]
  }

  class Impl extends Service {
    override def getData(path: String):Task[List[String]] = {
      ZIO.bracket(ZIO.effect(Source.fromFile(path)))(res => URIO(res.close())){ file =>
        ZIO.foreach(file.getLines().toList)(raw =>
          ZIO.succeed(raw)
        )
      }
    }
  }

  val live: ULayer[FileService] = ZLayer.succeed(new Impl)

  def getData(path: String): RIO[FileService,List[String]] = ZIO.accessM(_.get.getData(path))
}
