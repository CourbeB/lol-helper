package gimmeInfoLoL.plugin

import gimmeInfoLoL.helper.LolHelperContext
import gimmeInfoLoL.helper.ImplicitHelpers._
import sx.blah.discord.handle.obj.IMessage

import play.api.libs.json.Json
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by bcourbe on 27/02/2017.
  */
object Itemset {

  import scala.concurrent.ExecutionContext.Implicits._
  val correctPosition = Map("top"->"Top", "jungle"->"Jungle", "middle"->"Middle", "adc"->"ADC", "support"->"Support")

  case class ItemsSet(games: Int, winPercent: Double, items: Array[Int], role: String)
  implicit val itemsSetReads = Json.reads[ItemsSet]

  case class ChampionUnknow(error: String)
  implicit val championUnknowReads = Json.reads[ChampionUnknow]

  case class Item(id: Int, plaintext: String, description: String, name: String)
  implicit val itemReads = Json.reads[Item]

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
    val apiTokenChampionGG = LolHelperContext.apiKeyChampionGG
    val apiTokenLol = LolHelperContext.apiKeyLol
    val request1  = s"http://api.champion.gg/champion/$champ/items/starters/mostWins?api_key=$apiTokenChampionGG"

    def makeRequest(id: String): String={
      s"https://global.api.pvp.net/api/lol/static-data/euw/v1.2/item/$id?api_key=$apiTokenLol"
    }

    import gimmeInfoLoL.helper.LolHelperContext.implicits._

    val response = for {
      //Future
      itemsSetsF <- wsClient.url(request1).get().map {
        r => if (r.status == 200) Future.successful(r.json.as[Array[ItemsSet]]) else Future.failed(new Exception("Champion not found"))
      }
      itemsSets <- itemsSetsF
    } yield for{
      //Option
      items <- itemsSets.map(e => (e.role, e.items)).toMap.get(position)
    } yield for{
      //Array
      item <-items
    } yield for{
      //Future
      itemNameF <- wsClient.url(makeRequest(item.toString)).get().map{
        r=> if (r.status == 200) Future.successful((r.json \ "name").as[String]) else Future.failed(new Exception("Item not found"))
      }
      itemName <- itemNameF
    } yield itemName

    val formatedResponse = response.flatMap{
      case Some(e) => Future.sequence(e.toList)
      case None => Future.failed(new Exception("Role not found"))
    }

    formatedResponse.onComplete{
      case Success(msg) => message.post(formatAnswer(msg))
      case Failure(e) if e.getMessage == "Role not found" => unknowRole(message)
      case Failure(e) if e.getMessage == "Champion not found" => unknowChampion(message)
      case Failure(e) if e.getMessage == "Item not found" => unknowItem(message)
    }
  }

  def formatAnswer(itemList : List[String]): String ={
    s"You should start with ${itemList.mkString(", ")}."
  }

  def unknowChampion(message: IMessage)={
    message.post("`Unknown champion`")
  }

  def unknowItem(message: IMessage)={
    message.post("`Unknown item`")
  }

  def unknowRole(message: IMessage)={
    message.post("`Unknown role`")
  }

  def errorCommand(message: IMessage)={
    message.post("`You need to specify a correct champion and position (ie: !lol item quinn top)`")
  }
}
