package model

case class Event(id: String,
                 eventTime: Long,
                 source: Option[NetworkAsset],
                 destination: Option[NetworkAsset],
                 subject: Option[Subject],
                 `object`: Option[Object],
                 action: String,
                 AddField1: Option[String],
                 AddField2: Option[String],
                 listIoc: List[Int]
                ){
  def setListIoc(list: List[Int]): Event =
    this.copy(listIoc = list)
}


case class NetworkAsset(ip: Option[String],
                       port: Option[Int],
                       hostname: Option[String]
                       )

case class Subject(name: Option[String], category: Option[String])

case class Object(name: Option[String], path: Option[String], category: Option[String])
