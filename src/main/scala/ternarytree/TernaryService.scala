package ternarytree

import model.{Event, Ioc}
import zio._

import scala.collection.mutable


object TernaryService {
  type TernaryService = Has[Service]

  private val hashTree = new mutable.HashMap[String, TernaryTree[Ioc]]
    .addAll(Seq(
    ("subjectName", TernaryTree[Ioc]),
    ("objectName", TernaryTree[Ioc]),
    ("host", TernaryTree[Ioc])
  ))
  private val capabIoc = new mutable.HashMap[Int, Int]


  trait Service {
    def init(iocs: List[Ioc]): UIO[Unit]
    def search(events: List[Event]): UIO[List[(Event, List[Int])]]
  }


  class Impl extends Service {
    override def init(iocs: List[Ioc]): UIO[Unit] =
      ZIO.collect(iocs)(ioc =>
        ZIO.succeed(hashTree.update("subjectName", hashTree("subjectName")
          .insert(ioc.subject.login, ioc))).as(capabIoc.addOne(ioc.id, ioc.ctr())) *>
          ZIO.fromOption(ioc.`object`)
            .map(objIoc =>
            hashTree.update("objectName", hashTree("objectName")
              .insert(objIoc.name, ioc))).option *>
          ZIO.fromOption(ioc.host.flatMap(hostIoc =>
            hostIoc.host.orElse(hostIoc.ip)))
            .map(hostIoc =>
              hashTree.update("host", hashTree("host")
                .insert(hostIoc, ioc))
            )).unit

    override def search(events: List[Event]): UIO[List[(Event, List[Int])]] =
      ZIO.collectPar(events)(ev =>
        ZIO.collectPar(List(
          searchSubject(ev, "subjectName"),
          searchSubject(ev, "objectName"),
          searchSubject(ev, "hostS"),
          searchSubject(ev, "hostD")))
        (ZIO.fromOption(_))
          .map(listIoc =>
            ev -> filterIoc(listIoc)
        ))
  }

  val live: ULayer[TernaryService] = ZLayer.succeed(new Impl)


  def init(iocs: List[Ioc]): URIO[TernaryService,Unit] = ZIO.accessM(_.get.init(iocs))

  def search(events: List[Event]): URIO[TernaryService,List[(Event, List[Int])]] = ZIO.accessM(_.get.search(events))



  private def filterIoc(lstIocsId: List[Int]): List[Int] =
    lstIocsId.groupBy(identity)
      .view
      .mapValues(_.size)
      .filter{case (iocId, ctr) => ctr >= capabIoc(iocId)}
      .keys
      .toList


  private def searchSubject(ev: Event, str: String): Option[Int] = {
    str match {
      case "subjectName" =>
        for {
          subj <- ev.subject
          subName <- subj.name
          res <- hashTree("subjectName").search(subName)
        } yield res.id
      case "objectName" =>
        for {
          obj <- ev.`object`
          objName <- obj.name
          res <- hashTree("objectName").search(objName)
        } yield res.id
      case "hostS" =>
        for {
          sour <- ev.source
          hostname <- sour.hostname
          ip <- sour.ip
          res <- hashTree("host").search(ip).orElse( hashTree("host").search(hostname))
        } yield res.id
      case "hostD" =>
        for {
          dest <- ev.destination
          hostname <- dest.hostname
          ip <- dest.ip
          res <- hashTree("host").search(ip).orElse( hashTree("host").search(hostname))
        } yield res.id
      case _ => None
    }
  }
}
