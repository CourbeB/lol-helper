name := "LoLHepler"

version := "1.0"

scalaVersion := "2.11.8"

// Discord4J
resolvers += "jitpack" at "https://jitpack.io"


// pizza-eveapi
resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "com.github.austinv11"       %% "Discord4j"               % "2.7.0",
  "org.scalaj"                 %% "scalaj-http"             % "2.2.1",
  "moe.pizza"                  %% "eveapi"                  % "0.25",
  "com.typesafe.scala-logging" %% "scala-logging"           % "3.1.0",
  "com.typesafe.play"          %% "play-ahc-ws-standalone"  % "1.0.0-M3",
  "net.ruippeixotog"           %% "scala-scraper"           % "1.2.0",
  "com.typesafe"                % "config"                  % "1.3.1"
)