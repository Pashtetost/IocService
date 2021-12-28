import model._

package object db {

  case class IocModel(id: Int, subject: Int, host: Int, `object`: Int, `type`: String, add_field: String) {
    def setIoc(sub: IocSubject, obj: Option[IocObject], host: Option[IocHost]):Ioc =
      Ioc(
        id = id,
        subject = sub,
        `object` = obj,
        host = host,
        iocType = `type`
      )
  }

  case class SubjectModel(id: Int, login: String, email: Option[String], add_field:  Option[String]) {
    def setSubject(): IocSubject = IocSubject(
      login = login,
      email = email
    )
  }

  case class ObjectModel(id: Int, obj_name: String, obj_path: Option[String], obj_type: String, add_field: Option[String]) {
    def setObject(): IocObject = IocObject(
      name = obj_name,
      path = obj_path,
      objectType = obj_type
    )
  }

  case class HostModel(id: Int, hostname: Option[String], ip: Option[String], add_field: Option[String]) {
    def setHost(): IocHost = IocHost(
      ip = ip,
      host = hostname
    )
  }
}
