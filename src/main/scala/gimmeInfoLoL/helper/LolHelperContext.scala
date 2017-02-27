package gimmeInfoLoL.helper

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import play.api.libs.ws.ahc.StandaloneAhcWSClient

/**
  * Created by bcourbe on 27/02/2017.
  */
object LolHelperContext {

  //ConfigFactory.parseFile(new File(s"myconf.conf"))
  val applicationConf = ConfigFactory.parseResources(getClass, "/dev.conf")

  val apiKeyDiscord = applicationConf.getString("api.key.Discord")
  val apiKeyChampionGG = applicationConf.getString("api.key.ChampionGG")

  object implicits {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val wsClient = StandaloneAhcWSClient()
  }
}
