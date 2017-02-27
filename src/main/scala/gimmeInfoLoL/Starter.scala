package gimmeInfoLoL

import gimmeInfoLoL.helper.LolHelperContext

/**
  * Created by bcourbe on 23/02/2017.
  */
object Starter {
  def main(args: Array[String]) {

    //TODO add a launching helper
    val token = LolHelperContext.apiKeyDiscord

    new Coach(token)
  }
}
