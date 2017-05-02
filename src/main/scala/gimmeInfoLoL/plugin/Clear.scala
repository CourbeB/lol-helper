package gimmeInfoLoL.plugin

import gimmeInfoLoL.helper.ImplicitHelpers._

import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.DiscordException
import scala.collection.JavaConverters._
import scala.util.Try

/**
  * Created by bcourbe on 24/03/2017.
  */
object Clear {
  def apply(message: IMessage, botName: String): Unit={
    val messages = message.getChannel.getMessages
    val a = messages.toArray.map(_.asInstanceOf[IMessage])
    val res = a.filter(m => (m.getAuthor.getName == botName) || m.getContent.startsWith("!lol"))
        .filter(_.getTimestamp.isAfter(message.getTimestamp.minusDays(14))).toList.asJava

    try{
      messages.bulkDelete(res)
    } catch {
      case e: DiscordException => message.post("`There is no message to delete (I'm not able to remove messages that are at least 14 days old)`")
      case e: UnsupportedOperationException => message.post("`I'm not able to delete message in this channel`")

    }
  }
}
