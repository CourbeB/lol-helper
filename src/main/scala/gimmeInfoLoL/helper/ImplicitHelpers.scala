package gimmeInfoLoL.helper

import sx.blah.discord.handle.obj.{IChannel, IMessage}

/**
  * Created by bcourbe on 01/03/2017.
  */
object ImplicitHelpers {

  implicit class MessagePostUtil(m: IMessage) {
    def post(msg: String): Unit = {
      m.getChannel.sendMessage(msg)
    }
  }

  implicit class ErrorSender(error: Errors.Error) {
    def sendTo(ichannel: IChannel) = {
      ichannel.sendMessage(error.message)
    }
  }
}
