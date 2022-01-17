import comparison.CheckService
import comparison.CheckService.CheckService
import config.AppConfig
import json.JsonConverter
import json.JsonConverter.JsonConverter
import ternarytree.TernaryService
import ternarytree.TernaryService.TernaryService
import zio.clock.Clock
import zio.console.Console
import zio.{Has, IO, Ref, Schedule, URLayer, ZIO, ZLayer}
import zio.kafka.consumer._
import zio.kafka.serde.Serde
import zio.logging.{Logging, log}
import zio.kafka.producer.Producer
import zio.stream.ZStream


object PipelineService {
  type PipelineService = Has[Service]

  trait Service {
    def run: IO[Any, Unit]
  }

  val live: URLayer[Logging with Console with TernaryService with JsonConverter with CheckService with Has[Consumer] with Has[Producer] with Has[AppConfig] with Clock, PipelineService] =
    ZLayer.fromFunction(env =>
      new Service {
      override def run: IO[Any, Unit] = {
        val streamDb = ZStream.repeatEffectWith(CheckService.getIocs, Schedule.fixed(env.get[AppConfig].dao.zioDuration))
          .tapError(err => log.error(err.getMessage))
          .retry(Schedule.fixed(env.get[AppConfig].dao.zioDuration))

        val streamKafka = Consumer.subscribeAnd(Subscription.topics(env.get[AppConfig].kafka.consumer.topic))
          .plainStream(Serde.long, Serde.string)

         (for {
           start <- CheckService.getIocs
           iocDataRef <- Ref.make(start)
           _ <- streamDb
             .map(newIoc => iocDataRef.set(newIoc))
             .runDrain
             .delay(env.get[AppConfig].dao.zioDuration)
             .fork
           _ <- streamKafka
             .mapM{rdd => for{
               parsedEitherEvent <- JsonConverter.decodeStr(rdd.value)
               iocData <- iocDataRef.get
               anw <- parsedEitherEvent
                 .fold(err => for{
                   _ <- log.error(s"Error parsing $err")
                   _ <- Producer.produce(env.get[AppConfig].kafka.producer.topicError, rdd.key, err, Serde.long, Serde.string)
                 } yield rdd.offset,
                   parsedEvent => for{
                     iocEvent <- TernaryService.searchIocs(parsedEvent, iocData)
                     iocEventStr <- JsonConverter.encodeEvent(iocEvent)
                     _ <- log.info(s"Ioc Event: $iocEvent")
                     _ <- Producer.produce(env.get[AppConfig].kafka.producer.topicParsed, rdd.key, iocEventStr, Serde.long, Serde.string)
                   } yield rdd.offset)
             } yield anw}
             .aggregateAsync(Consumer.offsetBatches)
             .mapM(_.commit)
             .runDrain
        } yield ())
           .provide(env)
      }
      })

  def run: ZIO[PipelineService, Any, Unit] =
    ZIO.accessM(_.get.run)
}
