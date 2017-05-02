package gimmeInfoLoL.plugin

import cats._
import cats.implicits._
import cats.data.EitherT
import com.typesafe.scalalogging.LazyLogging
import gimmeInfoLoL.helper.LolHelperContext
import gimmeInfoLoL.helper.LolHelperContext._
import gimmeInfoLoL.helper.ImplicitHelpers._
import gimmeInfoLoL.helper.Errors._
import sx.blah.discord.handle.obj.{IChannel, IMessage}

import play.api.libs.json.Json
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by bcourbe on 27/02/2017.
  */
object Itemset extends LazyLogging {

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

    def getItemsSets(champ: String): EitherT[Future, Error, Array[ItemsSet]] = {
      val url = s"http://api.champion.gg/champion/$champ/items/starters/mostWins?api_key=$apiKeyChampionGG"
      EitherT(wsClient.url(url).get()
        .map(
          r =>
            if (r.status == 200) Right(r.json.as[Array[ItemsSet]])
            else Left(UnknowChampionError)
        ))
    }

    def getItemName(id: Int): EitherT[Future, Error, String] = {
      val url = s"https://global.api.pvp.net/api/lol/static-data/euw/v1.2/item/$id?api_key=$apiKeyLol"
      EitherT(wsClient.url(url).get()
        .map(
          r =>
            if (r.status == 200) Right((r.json \ "name").as[String])
            else Left(UnknowItemError)
        ))
    }

    def getItemsForPosition(arrItems: Array[ItemsSet], position: String): EitherT[Future, Error, List[Int]] = {
      val ids = arrItems.map(e => (e.role, e.items)).toMap.get(position)
      EitherT(ids match {
        case Some(arr) => Future.successful(Right(arr.toList))
        case None => Future.successful(Left(UnknowRoleError))
      })
    }

    def processItems(itemsList:List[EitherT[Future, Error, String]], channel: IChannel)={
      Future.sequence(itemsList.map(_.value)).onComplete{
        case Success(items) => items.sequenceU match {
          case Left(e:Error) => e sendTo channel
          case Right(itemsName) => channel.sendMessage(formatAnswer(itemsName))
        }
        case Failure(_) => logger.error("Error in processing the items")
      }
    }

    val result = for {
      itemsSets <- getItemsSets(champ)
      items <- getItemsForPosition(itemsSets, position) } yield for {
      item <- items } yield for {
      itemName <- getItemName(item)
    } yield itemName

    val channel = message.getChannel

    result.value.onComplete{
      case Success(Left(e:Error)) => e sendTo channel
      case Success(Right(items)) => processItems(items, channel)
      case Failure(_) => logger.error("Error in getting the items")
    }
  }

  def formatAnswer(itemList : List[String]): String ={
    s"You should start with ${itemList.mkString(", ")}."
  }

  def errorCommand(message: IMessage)={
    message.post("`You need to specify a correct champion and position (ie: !lol item quinn top)`")
  }
}
