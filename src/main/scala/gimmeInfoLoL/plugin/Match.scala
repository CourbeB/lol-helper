package gimmeInfoLoL.plugin

import gimmeInfoLoL.helper.Tabulator

import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.util.{Success, Failure}
import sx.blah.discord.handle.obj.{IChannel, IMessage}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.json._

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model.{Document, ElementQuery, Element}


/**
  * Created by bcourbe on 23/02/2017.
  */
object Match {

  import scala.concurrent.ExecutionContext.Implicits._
  case class LolNexusResponse(successful: Boolean, html: String)
  implicit val lolNexusResponseReads = Json.reads[LolNexusResponse]

  def apply(message: IMessage) = {
    val content = message.getContent
    val name = content.split(" ").drop(2).mkString("+")

    // Get id https://euw.api.pvp.net/api/lol/euw/v1.4/summoner/by-name/Courbix?api_key=

    import gimmeInfoLoL.helper.LolHelperContext.implicits._

    val response = wsClient.url(s"http://www.lolnexus.com/ajax/get-game-info/EUW.json?name=$name").get().map{
      r => r.json.as[LolNexusResponse]
    }

    response.onComplete{
      case Success(info) => answer(info, message.getChannel)
      case Failure(e) => println(e)
    }
  }

  def answer(lolNexusResponse: LolNexusResponse, channel: IChannel):Unit ={
    lolNexusResponse match {
      case LolNexusResponse(false, html) => answerNotInGame(html, channel)
      case LolNexusResponse(true, html) => answerInGame(html, channel)
    }
  }

  def answerNotInGame(s: String, channel: IChannel): Unit={
    val browser = JsoupBrowser()
    val doc = browser.parseString(s)
    val result = doc >> extractor("div", text)
    channel.sendMessage(result.mkString(""))
  }

  def answerInGame(s: String, channel: IChannel): Unit={
    val browser = JsoupBrowser()
    val doc = browser.parseString(s)

    channel.sendMessage(formatTeam(doc, 1))
    channel.sendMessage(formatTeam(doc, 2))
  }

  def formatTeam(doc: Document, team: Int) : String ={
    val namesTeam = "Name" +: (doc >> extractor(s"div .team-$team > table > tbody > tr > td.name", texts)).toList
    val champTeam = "Champion" +: (doc >> extractor(s"div .team-$team > table > tbody > tr > td.champion", texts)).toList
    val rankTeam = "Current" +: (doc >> extractor(s"div .team-$team > table > tbody > tr > td.current-season", texts)).toList
    val lastrRankTeam = "Last" +: (doc >> extractor(s"div .team-$team > table > tbody > tr > td.last-season", texts)).toList
    val winRateTeam = "Win Rate" +: (doc >> extractor(s"div .team-$team > table > tbody > tr > td.ranked-wins-losses", texts)).toList
    val kdaTeam = "KDA" +: (doc >> extractor(s"div .team-$team > table > tbody > tr > td.champion-kda", texts)).toList
    val masteriesTeam = "Masteries" +: (doc >> extractor(s"div.team-$team div.talent-tree-label", texts))
      .grouped(3).map(l=>l.map(_.split(" ")(0)).mkString("/")).toList
    val summonersTeam = "Summoners" +:
      (doc >> extractor(s"div .team-$team > table > tbody > tr > td.champion > div.summoner-spells > img", attrs("title")))
        .grouped(2).map(l=>l.map(_.substring(0,3)).mkString("/")).toList

    //val runesTeam1 = (doc >> extractor(s"div .team-1 > table > tbody > tr > td.runes > span.tip > div.tooltip-html > div", texts)).toList

    s"Team $team :\n```"+Tabulator.format(
      List(namesTeam, champTeam, rankTeam, lastrRankTeam, winRateTeam, kdaTeam, masteriesTeam, summonersTeam).transpose
    )+"```"
  }

  /*
  A TESTER
  lazy val zombieConnectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnection("localhost", 9001)

  def zombieRequest(request:HttpRequest): Future[HttpResponse] =
    Source.single(request).via(zombieConnectionFlow).runWith(Sink.head)

  def fetchZombieInfo(id: String) : Future[Either[String, Zombie]] = {
   zombieRequest(RequestBuilding.Get(s"/zombies/$id")).flatMap { response =>
     response.status match {
       case OK => Unmarshal(response.entity).to[Zombie].map(Right(_))
       case BadRequest => Future.successful(Left(s"bad request"))
       case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
         val error = s"FAIL - ${response.status}"
         Future.failed(new IOException(error))
       }
     }
   }
  }

  ET FAIRE UN onComplete sur le future et basta
  OU
  val uri = "http://www.yahoo.com"
  val reqEntity = Array[Byte]()

  val respEntity = for {
    request <- Marshal(reqEntity).to[RequestEntity]
    response <- Http().singleRequest(HttpRequest(method = HttpMethods.POST, uri = uri, entity = request))
    entity <- Unmarshal(response.entity).to[ByteString]
  } yield entity

  val payload = respEntity.andThen {
    case Success(entity) =>
      s"""{"content": "${entity.utf8String}"}"""
    case Failure(ex) =>
      s"""{"error": "${ex.getMessage}"}"""
  }
   */
}

