import comparison.CheckService
import config.AppConfig
import configuration.ConfigService
import dao.IocRepository
import io.getquill.context.ZioJdbc.DataSourceLayer
import json.JsonConverter
import ternarytree.TernaryService
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.logging._
import zio.logging.slf4j._
import zio.{ExitCode, Has, ULayer, URIO, ZLayer}
import zio.duration._
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.kafka.producer.{Producer, ProducerSettings}

object Main extends zio.App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val correlationId = LogAnnotation[Int](
      name = "correlationId",
      initialValue = 0,
      combine = (_, newValue) => newValue,
      render = _.toString
    )

    val loggerLayer = Logging.console(
      logLevel = LogLevel.Info,
      format = LogFormat.ColoredLogFormat((ctx, line) => s"${ctx(correlationId)} $line")
    )
    val dataSourceLayer = DataSourceLayer.fromPrefix("db")

    val kafkaLayer = Blocking.live ++ Clock.live ++ ConfigService.live >>> (consumerKafka ++ producerKafka)
    val checkLayer = ((dataSourceLayer >>> IocRepository.live) ++ TernaryService.live ++ loggerLayer) >>> CheckService.live
    val jsonLayer = loggerLayer >>> JsonConverter.live

    val env = loggerLayer ++ Console.live ++ checkLayer ++ jsonLayer ++ TernaryService.live ++ Clock.live ++ ConfigService.live ++ kafkaLayer >>> PipelineService.live

    PipelineService.run.provideSomeLayer(env)
      .exitCode
  }

  val consumerKafka: ZLayer[Clock with Blocking with Has[AppConfig], Throwable, Has[Consumer]] =  ZLayer.fromService[AppConfig, AppConfig](identity).flatMap{ conf =>
    val consumerSettings =
      ConsumerSettings(conf.get.kafka.consumer.brokers)
        .withGroupId(conf.get.kafka.consumer.groupId)
        .withClientId("client")
        .withCloseTimeout(30.seconds)
        .withPollTimeout(10.millis)
        .withProperty("enable.auto.commit", "false")
        .withProperty("auto.offset.reset", "earliest")
    Consumer.make(consumerSettings).toLayer
  }

  val producerKafka: ZLayer[Blocking with Has[AppConfig], Throwable, Has[Producer]] = ZLayer.fromService[AppConfig, AppConfig](identity).flatMap { conf =>
    val producerSettings =  ProducerSettings(conf.get.kafka.producer.brokers)
    Producer.make(producerSettings).toLayer
  }
}
