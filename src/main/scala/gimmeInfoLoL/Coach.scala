package gimmeInfoLoL

import gimmeInfoLoL.eventHandlers.{MentionReceivedHandler, MessageReceivedHandler}
import sx.blah.discord.api._

/**
  * Created by bcourbe on 23/02/2017.
  */
class Coach(token:String) {
  val client = new ClientBuilder().withToken(token).login()

  client.getDispatcher.registerListener(new MessageReceivedHandler())
}
