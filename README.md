# Lol-helper

[![Build Status](https://travis-ci.org/CourbeB/lol-helper.svg?branch=master)](https://travis-ci.org/CourbeB/lol-helper)

This bot is still on development with limited access (waiting for an official api key from League of Legend).

Available commands
------------------
```
!lol nexus summoner-name - Get rank, champ, winrate, and games for all players in a current match
!lol stalker summoner-name - Get all the OP.GG profiles in a current match
!lol best position - Get the top 10 best champs for a position [top, middle, jungle, adc, support]
!lol counter champion-name position - Get the top 10 counters for a Champion and Position
!lol items champ-name position - Get the highest win starting item sets for a Champion and Position
!lol help - Give the list of all commands
```

TODO
----
- [ ] Refactor `!lol nexus` (use official api rather than scraping lol-nexus)
- [ ] Refactor `!lol items` (use `EitherT` rather than multiple for comprehension)
- [ ] Add `!lol clear` to remove all messages from lol-helper in the current channel
- [ ] Add `!lol version`
- [ ] Refactor `Stalker` : create case class for error, create a new object for all requests
- [ ] Etc.