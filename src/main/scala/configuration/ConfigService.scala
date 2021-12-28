package configuration

import config.AppConfig
import zio.{Has, Layer}
import zio.config.ReadError
import zio.config.magnolia.DeriveConfigDescriptor.descriptor
import zio.config.typesafe.TypesafeConfig.fromDefaultLoader

object ConfigService {

  private val configDescriptor = descriptor[AppConfig]

  val live: Layer[ReadError[String], Has[AppConfig]] = fromDefaultLoader(configDescriptor)
}
