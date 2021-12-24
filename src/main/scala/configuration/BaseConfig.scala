package configuration

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging


class BaseConfig extends LazyLogging {
  protected val appConf: Config = ConfigFactory.load()

  import scala.jdk.CollectionConverters.SetHasAsScala

  logger.info(
    appConf
      .entrySet().asScala
      .filterNot(_.getKey.contains("pass"))
      .foldLeft("Starting application with parameters: \n")((res, e) => s"$res\t${e.getKey} = ${e.getValue.unwrapped()}\n")
  )

  lazy val dbConfig: Config = appConf.getConfig("db")

  lazy val path = appConf.getString("file.path")
}
