package gimmeInfoLoL.plugin

import cats._
import cats.implicits._
import cats.data.EitherT
import gimmeInfoLoL.helper.LolHelperContext
import gimmeInfoLoL.helper.LolHelperContext._
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

    import gimmeInfoLoL.helper.LolHelperContext.implicits._

    def getItemsSets(champ: String): EitherT[Future, String, Array[ItemsSet]] = {
      val url = s"http://api.champion.gg/champion/$champ/items/starters/mostWins?api_key=$apiKeyChampionGG"
      EitherT(wsClient.url(url).get()
        .map(
          r =>
            if (r.status == 200) Right(r.json.as[Array[ItemsSet]])
            else Left("Champion not found")
        ))
    }

    def getItemName(id: Int): EitherT[Future, String, String] = {
      val url = s"https://global.api.pvp.net/api/lol/static-data/euw/v1.2/item/$id?api_key=$apiKeyLol"
      EitherT(wsClient.url(url).get()
        .map(
          r =>
            if (r.status == 200) Right((r.json \ "name").as[String])
            else Left("Item not found")
        ))
    }

    def getItemsForPosition(arrItems: Array[ItemsSet], position: String): EitherT[Future, String, List[Int]] = {
      val ids = arrItems.map(e => (e.role, e.items)).toMap.get(position)
      EitherT(ids match {
        case Some(arr) => Future.successful(Right(arr.toList))
        case None => Future.successful(Left("Role not found"))
      })
    }

    def processItems(itemsList:List[EitherT[Future, String, String]], message: IMessage)={
      Future.sequence(itemsList.map(_.value)).onComplete{
        case Success(items) => items.sequenceU match {
          case Left(e) => unknowItem(message)
          case Right(itemsName) => message.post(formatAnswer(itemsName))
        }
        case Failure(_) =>
      }
    }

    val result = for {
      itemsSets <- getItemsSets(champ)
      items <- getItemsForPosition(itemsSets, position) } yield for {
      item <- items } yield for {
      itemName <- getItemName(item)
    } yield itemName

    result.value.onComplete{
      case Success(Left(e)) if e == "Champion not found" => unknowChampion(message)
      case Success(Left(e)) if e == "Item not found" => unknowItem(message)
      case Success(Left(e)) if e == "Role not found" => unknowRole(message)
      case Success(Right(items)) => processItems(items, message)
      case Failure(_) =>
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
