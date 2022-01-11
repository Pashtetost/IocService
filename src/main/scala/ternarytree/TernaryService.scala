package ternarytree

import model.{Event, Ioc, IocData}
import zio._


object TernaryService {
  type TernaryService = Has[Service]


  trait Service {
    def initIocData(iocs: List[Ioc]): UIO[IocData]

    def searchListIoc(events: List[Event]): URIO[IocData, List[(Event, List[Int])]]

    def searchIocs(event: Event, iocData: IocData): UIO[(Event, List[Int])]
  }

  class Impl extends Service {
    override def initIocData(iocs: List[Ioc]): UIO[IocData] =
      for {
        hashTree2 <- Ref.make(Map(("subjectName", TernaryTree[Ioc]),
          ("objectName", TernaryTree[Ioc]),
          ("host", TernaryTree[Ioc])))
        capabIoc2 <- Ref.make(Map.empty[Int, Int])
        _ <- ZIO.collect(iocs)(ioc =>
          for {
            _ <- capabIoc2.update(_.updated(ioc.id, ioc.ctr()))
            _ <- hashTree2.update(tree => tree.updated("subjectName", tree("subjectName").insert(ioc.subject.login, ioc)))
            _ <- hashTree2.update(tree => ioc.`object`.map(objIoc =>
              tree.updated("objectName", tree("objectName").insert(objIoc.name, ioc))).getOrElse(tree))
            _ <- hashTree2.update(tree => ioc.host.flatMap(hostIoc =>
              hostIoc.host.orElse(hostIoc.ip))
              .map(hostIoc => tree.updated("host", tree("host").insert(hostIoc, ioc))).getOrElse(tree))
          } yield ()).unit
      } yield IocData(hashTree2, capabIoc2)

    override def searchListIoc(events: List[Event]): URIO[IocData, List[(Event, List[Int])]] =
      ZIO.accessM[IocData] { iocData =>
        for {
          hashTree2 <- iocData.hashTree.get
          capabIoc2 <- iocData.capabIoc.get
          res <- ZIO.collectPar(events)(ev =>
            ZIO.collectPar(List(
              searchAttribute(ev, "subjectName", hashTree2),
              searchAttribute(ev, "objectName", hashTree2),
              searchAttribute(ev, "hostS", hashTree2),
              searchAttribute(ev, "hostD", hashTree2)))
            (ZIO.fromOption(_))
              .map(listIoc =>
                ev -> filterIoc(listIoc, capabIoc2)
              ))
        } yield res
      }

    override def searchIocs(event: Event, iocData: IocData): UIO[(Event, List[Int])] =
        for {
          hashTree <- iocData.hashTree.get
          capabIoc <- iocData.capabIoc.get
          res <- ZIO.collectPar(List(searchAttribute(event, "subjectName", hashTree),
            searchAttribute(event, "objectName", hashTree),
            searchAttribute(event, "hostS", hashTree),
            searchAttribute(event, "hostD", hashTree)))(ZIO.fromOption(_))
            .map(listIoc => event -> filterIoc(listIoc, capabIoc))
        } yield res
  }

  val live: ULayer[TernaryService] = ZLayer.succeed(new Impl)

  def searchIocs(event: Event, iocData: IocData): URIO[TernaryService, (Event, List[Int])] = ZIO.accessM(_.get.searchIocs(event, iocData))

  /**
   * Helpers for correct check Iocs
   *
   */

    private def filterIoc(lstIocsId: List[Int], capabIoc: Map[Int, Int]): List[Int] =
      lstIocsId.groupBy(identity)
        .view
        .mapValues(_.size)
        .filter { case (iocId, ctr) => ctr >= capabIoc(iocId) }
        .keys
        .toList


    private def searchAttribute(ev: Event, str: String, hashTree: Map[String, TernaryTree[Ioc]]): Option[Int] =
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
            res <- hashTree("host").search(ip).orElse(hashTree("host").search(hostname))
          } yield res.id
        case "hostD" =>
          for {
            dest <- ev.destination
            hostname <- dest.hostname
            ip <- dest.ip
            res <- hashTree("host").search(ip).orElse(hashTree("host").search(hostname))
          } yield res.id
        case _ => None
      }
}
