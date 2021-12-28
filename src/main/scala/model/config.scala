import config.AppConfig.FileConfig

package object config {
  final case class AppConfig (file: FileConfig)

  object AppConfig {
    final case class FileConfig(path: String)
  }

}
