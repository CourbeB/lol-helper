package gimmeInfoLoL.plugin

import gimmeInfoLoL.helper.LolHelperContext
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

//    val response = wsClient.url(request1).get().map{
//      r =>
//        if(r.status == 200)
//          Right(r.json.as[Array[ItemsSet]])
//        else Left(r.json.as[ChampionUnknow])
//    }

    val response = for {
      //Future
      itemsSets <- wsClient.url(request1).get().map {
        r => if (r.status == 200) Future.successful(r.json.as[Array[ItemsSet]]) else Future.failed(new Exception)
      }
      test <- itemsSets
    } yield for{
      //Option
      items <- test.map(e => (e.role, e.items)).toMap.get(position)
    } yield for{
      //Array
      item <-items
    } yield for{
      //Future
      itemName <- wsClient.url(makeRequest(item.toString)).get().map{r=>r.json.as[Item]}
    } yield itemName.name

    val formatedResponse = response.flatMap{
      case Some(e) => Future.sequence(e.toList)
    }

    formatedResponse.onComplete{
      case Success(msg) => println(msg.mkString(", "))
      case Failure(e) => println(e)
    }

//    response.onComplete{
//      case Success(Right(info)) => message.getChannel.sendMessage(formatAnswer(info, position))
//      case Success(Left(_)) => unknowChampion(message)
//      case Failure(e) => println(e)
//    }
  }

  def unknowChampion(message: IMessage)={
    message.getChannel.sendMessage("`Unknown champion`")
  }

  def errorCommand(message: IMessage)={
    message.getChannel.sendMessage("`You need to specify a correct champion and position (ie: !lol item quinn top)`")
  }
}
