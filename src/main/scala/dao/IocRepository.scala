package dao

import db._
import io.getquill.{PostgresZioJdbcContext, SnakeCase}
import zio._

import java.sql.SQLException
import javax.sql.DataSource


object IocRepository {
  type IocRepository = Has[Service]

  val ctx = new PostgresZioJdbcContext(SnakeCase)
  import ctx._


  trait Service{
    def getSubjects: IO[SQLException, List[SubjectModel]]
    def getObjects: IO[SQLException, List[ObjectModel]]
    def getHosts: IO[SQLException, List[HostModel]]
    def getIocs: IO[SQLException, List[(((IocModel, SubjectModel), Option[ObjectModel]), Option[HostModel])]]
  }

  class Impl(dataSource: Has[DataSource]) extends Service {
    val iocs = quote{
      querySchema[IocModel](""""iocs"""")
    }
    val subjects = quote{
      querySchema[SubjectModel](""""subject"""")
    }
    val objects = quote{
      querySchema[ObjectModel](""""object"""")
    }
    val hosts = quote{
      querySchema[HostModel](""""hosts"""")
    }

    def getSubjects: IO[SQLException, List[SubjectModel]] =
      ctx.run(subjects).provide(dataSource)

    def getObjects: IO[SQLException, List[ObjectModel]] =
      ctx.run(objects).provide(dataSource)

    def getHosts: IO[SQLException, List[HostModel]] =
      ctx.run(hosts).provide(dataSource)

    def getIocs: IO[SQLException, List[(((IocModel, SubjectModel), Option[ObjectModel]), Option[HostModel])]] =
      ctx.run(iocs
        .join(subjects).on(_.subject == _.id)
        .leftJoin(objects).on(_._1.`object` == _.id)
        .leftJoin(hosts).on(_._1._1.host == _.id)
      ).provide(dataSource)
  }

  val live: ZLayer[Has[DataSource], Nothing, Has[Service]] = ZLayer.fromFunction[Has[DataSource], IocRepository.Service](dataSource => new Impl(dataSource))


  def getIocs:ZIO[IocRepository, Throwable, List[(((IocModel, SubjectModel), Option[ObjectModel]), Option[HostModel])]] =
    ZIO.accessM(_.get.getIocs)
}
