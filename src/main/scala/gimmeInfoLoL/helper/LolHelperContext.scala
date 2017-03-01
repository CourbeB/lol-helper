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

  //ConfigFactory.parseFile(new File(s"myconf.conf"))
  val applicationConf = ConfigFactory.parseResources(getClass, "/dev.conf")

  val apiKeyDiscord = envOrElse("DISCORD", applicationConf.getString("api.key.Discord"))
  val apiKeyChampionGG = envOrElse("CHAMPION_GG", applicationConf.getString("api.key.ChampionGG"))
  val apiKeyLol = envOrElse("LOL", applicationConf.getString("api.key.LoL"))

  object implicits {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val wsClient = StandaloneAhcWSClient()
  }
}
