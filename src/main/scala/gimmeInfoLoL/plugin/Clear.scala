package gimmeInfoLoL.plugin

import sx.blah.discord.handle.obj.IMessage
import scala.collection.JavaConverters._

/**
  * Created by bcourbe on 24/03/2017.
  */
object Clear {
  def apply(message: IMessage, botName: String): Unit={
    val messages = message.getChannel.getMessages
    val a = messages.toArray.map(_.asInstanceOf[IMessage])
    val res = a.filter(m => (m.getAuthor.getName == botName) || m.getContent.startsWith("!lol"))
      .filter(_.getTimestamp.isAfter(message.getTimestamp.minusDays(14))).toList.asJava
    messages.bulkDelete(res)
  }
}
