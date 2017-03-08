package gimmeInfoLoL.plugin

import akka.actor.FSM.Failure
import gimmeInfoLoL.helper.ImplicitHelpers._
import gimmeInfoLoL.helper.LolHelperContext._

import sx.blah.discord.handle.obj.IMessage
import play.api.libs.json.Json
import cats._
import cats.implicits._
import cats.data.EitherT

import scala.concurrent.Future
import scala.util.Success


/**
  * Created by bcourbe on 06/03/2017.
  */
object Stalker {

  import scala.concurrent.ExecutionContext.Implicits.global

  case class Summoner(id: Long, name: String, profileIconId: Int, revisionDate: Long, summonerLevel: Int)
  implicit val summonerReads = Json.reads[Summoner]

  case class Mastery(masteryId: Long, rank: Int)
  implicit val masteryReads = Json.reads[Mastery]

  case class Rune(count: Int, runeId: Long)
  implicit val runeReads = Json.reads[Rune]

  case class CurrentGameParticipant(bot: Boolean,
                                    championId: Long,
                                    masteries: List[Mastery],
                                    profileIconId: Long,
                                    runes: List[Rune],
                                    spell1Id: Long,
                                    spell2Id: Long,
                                    summonerId: Long,
                                    summonerName: String,
                                    teamId: Long)
  implicit val currentGameParticipantReads = Json.reads[CurrentGameParticipant]

  def apply(message: IMessage) = {
    val content = message.getContent
    val name = content.split(" ").drop(2).mkString("+")

    import gimmeInfoLoL.helper.LolHelperContext.implicits._

    def getSummoner(name: String): EitherT[Future, String, Summoner] = {
      val url = s"https://euw.api.pvp.net/api/lol/euw/v1.4/summoner/by-name/${name.replace(" ", "%20")}?api_key=$apiKeyLol"
      EitherT(wsClient.url(url).get()
        .map {
          r =>
            if (r.status == 200) Right((r.json \ name.replace(" ", "").toLowerCase).as[Summoner])
            else Left("summoner not found")
        })
    }


    def getGameInfo(id: String): EitherT[Future, String, Array[CurrentGameParticipant]] ={
      val url2 = s"https://euw.api.pvp.net/observer-mode/rest/consumer/getSpectatorGameInfo/EUW1/$id?api_key=$apiKeyLol"
      EitherT(wsClient.url(url2).get()
        .map {
          r =>
            if (r.status == 200) Right((r.json \ "participants").as[Array[CurrentGameParticipant]])
            else Left("game not found")
        })
    }

    val result = for {
      summoner <- getSummoner(name)
      gameInfo <- getGameInfo(summoner.id.toString)
    } yield gameInfo


    result.value.onComplete{
      case Success(Left(error)) if error == "summoner not found" => unknowSummoner(message)
      case Success(Left(error)) if error == "game not found" => summonerNotInGame(message)
      case Success(Right(gameParticipants)) => message.post(formatAnswer(gameParticipants))
    }
  }

  def formatAnswer(participants: Array[CurrentGameParticipant]) = {
    val nameAndUrl = participants.map{s =>
      val name = s.summonerName
      val url = s"<https://euw.op.gg/summoner/userName=${name.replace(" ", "%20")}>"
      s"$name -> $url"}
    nameAndUrl.mkString("\n")
  }

  def unknowSummoner(message: IMessage) = message.post("Summoner not found.")

  def summonerNotInGame(message: IMessage) = message.post("The summoner is not currently in a game.")

}
