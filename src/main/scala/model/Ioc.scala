package model


case class Ioc(id: Int, subject: IocSubject, `object`: Option[IocObject], host: Option[IocHost], iocType: String){
  def ctr(): Int = {
    var i = 1
    if (`object`.isDefined) i = i+1
    if (host.isDefined) i = i+1
    i
  }
}

case class IocSubject(login: String, email: Option[String])

case class IocObject(name: String,  path: Option[String], objectType: String)

case class IocHost(ip: Option[String], host: Option[String]) {

}