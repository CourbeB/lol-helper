package gimmeInfoLoL.helper

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.util.Properties.envOrElse

/**
  * Created by bcourbe on 27/02/2017.
  */
object LolHelperContext {

  case class Config(apiKeyDiscord: String, apiKeyChampionGG: String, apiKeyLol: String)

  val config = envOrElse("environment", "prod") match {
    case env if env=="prod" => Config(sys.env("DISCORD"), sys.env("CHAMPION_GG"), sys.env("LOL"))
    case env if env=="dev" =>
      val applicationConf = ConfigFactory.parseResources(getClass, s"/$env.conf")
      Config(
        applicationConf.getString("api.key.Discord"),
        applicationConf.getString("api.key.ChampionGG"),
        applicationConf.getString("api.key.LoL")
      )
  }

  val apiKeyDiscord = config.apiKeyDiscord
  val apiKeyChampionGG = config.apiKeyChampionGG
  val apiKeyLol = config.apiKeyLol

  object implicits {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val wsClient = StandaloneAhcWSClient()
  }
}
