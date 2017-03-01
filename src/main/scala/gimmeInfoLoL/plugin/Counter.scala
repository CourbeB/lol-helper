package gimmeInfoLoL.plugin

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import gimmeInfoLoL.helper.{LolHelperContext, Tabulator}
import gimmeInfoLoL.helper.ImplicitHelpers._
import play.api.libs.json.Json
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import sx.blah.discord.handle.obj.IMessage

import scala.util.{Failure, Success}

/**
  * Created by bcourbe on 27/02/2017.
  */
object Counter {

  import scala.concurrent.ExecutionContext.Implicits._
  val correctPosition = Map("top"->"Top", "jungle"->"Jungle", "middle"->"Middle", "adc"->"ADC", "support"->"Support")

  case class Matchup(games: Int, statScore: Double, winRate: Double, winRateChange: Double, key: String)
  implicit val matchupReads = Json.reads[Matchup]

  case class CounterMatchup(role: String, matchups: Array[Matchup])
  implicit val counterMatchupReads = Json.reads[CounterMatchup]

  case class ChampionUnknow(error: String)
  implicit val championUnknowReads = Json.reads[ChampionUnknow]

  def apply(message: IMessage): Unit={
    val content = message.getContent.split(" ").lift
    val champ = content(2)
    val position = content(3).map(_.toLowerCase)

    (champ, position) match {
      case (Some(c), Some(p)) => answer(c, correctPosition(p), message)
      case (_, _) => errorCommand(message)
    }
  }

  def answer(champ: String, position: String, message: IMessage)={
    val apiToken = LolHelperContext.apiKeyChampionGG
    val request  = s"http://api.champion.gg/champion/$champ/matchup?api_key=$apiToken"

    import gimmeInfoLoL.helper.LolHelperContext.implicits._

    val response = wsClient.url(request).get().map{
      r =>
        if(r.status == 200)
          Right(r.json.as[Array[CounterMatchup]])
        else Left(r.json.as[ChampionUnknow])
    }

    response.onComplete{
      case Success(Right(info)) => message.post(formatAnswer(info, position))
      case Success(Left(_)) => unknowChampion(message)
      case Failure(e) => println(e)
    }
  }

  def formatAnswer(infos: Array[CounterMatchup], position: String): String={
    val roleMatchups = infos.map(m => (m.role, m.matchups)).toMap
    roleMatchups.get(position) match {
      case Some(matchups) =>
        val bestCounter = matchups.sortWith(_.winRate<_.winRate).take(10)
        val champs = "Champion" +: bestCounter.map(_.key)
        val winRates = "Win rate" +: bestCounter.map(_.winRate.toString)
        "```" + Tabulator.format(List(champs, winRates).transpose) + "```"
      case None =>
        val availableRoles = roleMatchups.keys.map(_.toLowerCase()).mkString(", ")
        s"`Unknown position for this champion, the available positions are $availableRoles`"
    }
  }

  def unknowChampion(message: IMessage)={
    message.post("`Unknown champion`")
  }

  def unknownPosition(message: IMessage)={
    message.post(s"`Unknown position, correct positions are ${correctPosition.keys.mkString(", ")}.`")
  }

  def errorCommand(message: IMessage)={
    message.post("`You need to specify a correct champion and position (ie: !lol counter quinn top)`")
  }
}
