import config.AppConfig
import dao.IocRepository
import json.JsonConverter
import model.Event
import ternarytree.TernaryService
import zio.logging.{ Logging, log}
import zio.{Has, RIO, Task, ZIO, ZLayer}

object CheckService {
  type CheckService = Has[Service]

  trait Service{
    def check: RIO[Has[AppConfig.FileConfig] with Logging, List[(Event, List[Int])]]
  }

  class Impl(converter: JsonConverter.Service, repository: IocRepository.Service, ternary: TernaryService.Service) extends Service {
     def check: RIO[Has[AppConfig.FileConfig] with Logging, List[(Event, List[Int])]] = for {
       events <- converter.parse
       iocsAdapter <- repository.getIocs
       _ <- log.info(s"get data from DB")
       iocsR <- ZIO.effect(
         iocsAdapter.map{
           case (((ioc, subj), obj), host) => ioc.setIoc(subj.setSubject(), obj.map(_.setObject()), host.map(_.setHost()))
         })
       _ <- ternary.init(iocsR)
       _ <- log.info(s"start check events")
       res <- ternary.search(events)
       anw = res.filter(_._2.nonEmpty)
       _ <- ZIO.collect(anw){case (ev, num) => log.info(s"event: $ev\n Ioc IDs: $num")}
     } yield anw
  }



  val live = ZLayer.fromServices[JsonConverter.Service, IocRepository.Service, TernaryService.Service, CheckService.Service](
    (conventer, repository, ternary) => new Impl(conventer, repository, ternary))

  def check: RIO[CheckService with Has[AppConfig.FileConfig] with Logging, List[(Event, List[Int])]] = ZIO.accessM(_.get.check)
}
