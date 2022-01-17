package comparison

import dao.IocRepository
import dao.IocRepository.IocRepository
import filereader.FileService
import filereader.FileService.FileService
import json.JsonConverter
import json.JsonConverter.JsonConverter
import model.{Event, IocData}
import ternarytree.TernaryService
import ternarytree.TernaryService.TernaryService
import zio.logging.{Logging, log}
import zio.{Has, RIO, Task, URLayer, ZIO, ZLayer}

object CheckService {
  type CheckService = Has[Service]

  trait Service {
    def check: RIO[FileService with JsonConverter, List[(Event, List[Int])]]
    def getIocs: Task[IocData]
  }

  val live: URLayer[IocRepository with TernaryService with Logging, CheckService] = ZLayer.fromFunction(env => new Service {
    override def check: RIO[FileService with JsonConverter, List[(Event, List[Int])]] =
      (for {
        rawList <- FileService.getData
        events <- JsonConverter.decodeList(rawList)
        iocsAdapter <- IocRepository.getIocs
        _ <- log.info(s"get data from DB for list")
        iocsR <- ZIO.effect(
          iocsAdapter.map {
            case (((ioc, subj), obj), host) => ioc.setIoc(subj.setSubject(), obj.map(_.setObject()), host.map(_.setHost()))
          })
        iocData <- TernaryService.initIocData(iocsR)
        _ <- log.info(s"start check events")
        res <- TernaryService.searchListIoc(events, iocData)
        anw = res.filter(_._2.nonEmpty)
        _ <- ZIO.collect(anw) { case (ev, num) => log.info(s"event: $ev\n Ioc IDs: $num") }
      } yield anw).provideSome[FileService with JsonConverter](x => x ++ env)


    override def getIocs: Task[IocData] =
      (for {
        _ <- log.info(s"Try to get Ioc from DB")
        iocsAdapter <- IocRepository.getIocs
        iocsR <- ZIO.effect(
          iocsAdapter.map {
            case (((ioc, subj), obj), host) => ioc.setIoc(subj.setSubject(), obj.map(_.setObject()), host.map(_.setHost()))
          })
        iocData <- TernaryService.initIocData(iocsR)
        _ <- log.info(s"Success get Ioc")
      } yield iocData).provide(env)
  })

  def check: RIO[CheckService with FileService with JsonConverter, List[(Event, List[Int])]] = ZIO.accessM(_.get.check)

  def getIocs: RIO[CheckService, IocData] = ZIO.accessM(_.get.getIocs)
}
