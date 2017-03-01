package gimmeInfoLoL.plugin

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import gimmeInfoLoL.helper.{LolHelperContext, Tabulator}
import gimmeInfoLoL.helper.ImplicitHelpers._
import play.api.libs.json._
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import sx.blah.discord.handle.obj.{IChannel, IMessage}


import scala.util.{Failure, Success}

/**
  * Created by bcourbe on 26/02/2017.
  */
object BestChampPosition {

  import scala.concurrent.ExecutionContext.Implicits._
  val correctPosition = List("top", "jungle", "middle", "adc", "support")

  case class ChampInfo (overallPositionChange: Int,
                        overallPosition: Int,
                        goldEarned: Int,
                        neutralMinionsKilledEnemyJungle: Double,
                        neutralMinionsKilledTeamJungle: Double,
                        minionsKilled: Double,
                        largestKillingSpree: Double,
                        totalHeal: Int,
                        totalDamageTaken: Int,
                        totalDamageDealtToChampions: Int,
                        assists: Double,
                        deaths: Double,
                        kills: Double,
                        experience: Double,
                        banRate: Double,
                        playPercent: Double,
                        winPercent: Double)
  implicit val champInfoReads = Json.reads[ChampInfo]

  case class bestChampResponse(key: String, role: String, name: String, general: ChampInfo)
  implicit val bestChampResponseReads = Json.reads[bestChampResponse]

  def apply(message: IMessage): Unit ={
    val position = message.getContent.split(" ").lift(2)

    position match {
      case Some(p) if correctPosition.contains(p.toLowerCase()) => answer(p.toLowerCase(), message)
      case _ => unknownPosition(message)
    }
  }

  def answer(position: String, message: IMessage) ={
    val apiToken = LolHelperContext.apiKeyChampionGG
    val request  = s"http://api.champion.gg/stats/role/$position/mostWinning?api_key=$apiToken&page=1&limit=10"

    import gimmeInfoLoL.helper.LolHelperContext.implicits._

    val response = wsClient.url(request).get().map{
      r => (r.json \ "data").as[Array[bestChampResponse]]
    }

    response.onComplete{
      case Success(info) => message.post(formatAnswer(info))
      case Failure(e) => println(e)
    }
  }

  def formatAnswer(champs: Array[bestChampResponse]) ={
    val champNames = "Champion" +: champs.map(champ => champ.name)
    val winRates = "Win rate" +: champs.map(champ => champ.general.winPercent.toString)
    "```" + Tabulator.format(List(champNames, winRates).transpose) + "```"
  }

  def unknownPosition(message: IMessage)={
    message.post(s"`Unknown position, correct positions are ${correctPosition.mkString(", ")}.`")
  }

}
