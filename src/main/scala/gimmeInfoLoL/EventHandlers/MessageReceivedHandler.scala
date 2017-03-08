package gimmeInfoLoL.eventHandlers

import com.typesafe.scalalogging.LazyLogging
import gimmeInfoLoL.plugin._
import sx.blah.discord.api.events.IListener
import sx.blah.discord.handle.impl.events.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage

/**
  * Created by bcourbe on 23/02/2017.
  */
class MessageReceivedHandler extends IListener[MessageReceivedEvent] with LazyLogging {

  override def handle(event: MessageReceivedEvent) = {
    val message = event.getMessage
    val content = message.getContent
    val author = message.getAuthor

    logger.info(s"Received message from $author : $content")

    content match {
      case lolnexus if lolnexus.startsWith("!lol nexus") => fakeTyping(message, Match.apply)
      case stalker if stalker.startsWith("!lol stalker") => fakeTyping(message, Stalker.apply)
      case bestPosition if bestPosition.startsWith("!lol best") => fakeTyping(message, BestChampPosition.apply)
      case counter if counter.startsWith("!lol counter") => fakeTyping(message, Counter.apply)
      case itemset if itemset.startsWith("!lol items") => fakeTyping(message, Itemset.apply)
      case help if help.startsWith("!lol help") => fakeTyping(message, printHelp)
      case withoutCommand if withoutCommand.startsWith("!lol") => message.getChannel.sendMessage("`Unknown command`")
        fakeTyping(message, printHelp)
      case _ =>
    }
  }


  def fakeTyping(message:IMessage, f: IMessage => Unit): Unit = {
    message.getChannel.setTypingStatus(true)
    f(message)
    message.getChannel.setTypingStatus(false)
  }

  def printHelp(message: IMessage): Unit = {
    val man = """
      |```
      |Here is the list of all available commands :
      |!lol nexus summoner-name - Get rank, champ, winrate, and games for all players in a current match
      |!lol stalker summoner-name - Get all the OP.GG profiles in a current match
      |!lol best position - Get the top 10 best champs for a position [top, middle, jungle, adc, support]
      |!lol counter champion-name position - Get the top 10 counters for a Champion and Position
      |!lol items champ-name position - Get the highest win starting item sets for a Champion and Position
      |!lol help - Give the list of all commands
      |```
    """.stripMargin
    message.getChannel.sendMessage(man)
  }

  //  !lol bans - Get the top 10 most common bans
  // TODO http://api.champion.gg/docs/#api-Champion-GetChampionMostPopularSummoners
  //  !lol items champ-name position - Get the highest win item sets for a Champion and Position
  //  !lol skills champ-name position - Get the highest win skills for a Champion and Position
  //  !lol status - Get the LoL Game and Client server status for all regions
}
