import dao.IocRepository
import json.JsonConverter
import model.Event
import ternarytree.TernaryService
import zio.{Has, RIO, Task, ZIO, ZLayer}

object CheckService {
  type CheckService = Has[Service]

  trait Service{
    def check(path: String): Task[List[(Event, List[Int])]]
  }

  class Impl(converter: JsonConverter.Service, repository: IocRepository.Service, ternary: TernaryService.Service) extends Service {
     def check(path: String): Task[List[(Event, List[Int])]] = for {
       events <- converter.parse(path)
       iocsAdapter <- repository.getIocs
       iocsR <- ZIO.effect(
         iocsAdapter.map{
           case (((ioc, subj), obj), host) => ioc.setIoc(subj.setSubject(), obj.map(_.setObject()), host.map(_.setHost()))
         })
       _ <- ternary.init(iocsR)
       res <- ternary.search(events)
     } yield res.filter(_._2.nonEmpty)
  }



  val live = ZLayer.fromServices[JsonConverter.Service, IocRepository.Service, TernaryService.Service, CheckService.Service](
    (conventer, repository, ternary) => new Impl(conventer, repository, ternary))

  def check(path: String): RIO[CheckService, List[(Event, List[Int])]] = ZIO.accessM(_.get.check(path))
}
