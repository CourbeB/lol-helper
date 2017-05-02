package gimmeInfoLoL.helper

import sx.blah.discord.handle.obj.IMessage
import gimmeInfoLoL.helper.ImplicitHelpers._

/**
  * Created by bcourbe on 24/03/2017.
  */
object Errors {
  abstract class Error(val message: String)

  case object SummonerNotFoundError extends Error("Summoner not found.")

  case object SummonerNotInGameError extends Error("The summoner is not currently in a game.")

  case object UnknowChampionError extends Error("Unknown champion")

  case object UnknowItemError extends Error("Unknown item")

  case object UnknowRoleError extends Error("Unknown role")
}

