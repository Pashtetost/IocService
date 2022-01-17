import config.AppConfig.{DataBase, FileEvents, KafkaDescription}

import scala.concurrent.duration.Duration
import zio.duration


package object config {
  final case class AppConfig (file: FileEvents, kafka: KafkaDescription, dao: DataBase)

  object AppConfig {
    final case class FileEvents(path: String)

    final case class KafkaDescription(consumer: Consumer, producer: Producer)

    final case class Consumer(bootstrapServers: String, topic: String, groupId: String) {
      def brokers: List[String] = bootstrapServers.split(",").toList
    }

    final case class Producer(bootstrapServers: String, topicParsed: String, topicError: String) {
      def brokers: List[String] = bootstrapServers.split(",").toList
    }

    final case class DataBase(connectFreq: Duration) {
      def zioDuration: duration.Duration = duration.Duration.fromScala(connectFreq)
    }
  }

}
