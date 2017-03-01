package gimmeInfoLoL.eventHandlers

import sx.blah.discord.api.events.IListener
import sx.blah.discord.handle.impl.events.MentionEvent

/**
  * Created by bcourbe on 01/03/2017.
  */
class MentionReceivedHandler extends IListener[MentionEvent]{

  override def handle(event: MentionEvent)={
    val message = event.getMessage
    message.getChannel.sendMessage("Copy that!")
  }

}
