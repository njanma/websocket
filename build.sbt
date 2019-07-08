name := "websocket"

version := "0.1"

scalaVersion := "2.12.8"

scalacOptions += "-Ypartial-unification"

lazy val circeVersion = "0.10.0"
lazy val doobieVersion = "0.6.0"

libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core",
  "org.tpolecat" %% "doobie-postgres",
  "org.tpolecat" %% "doobie-specs2"
).map(_ % doobieVersion)
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.8"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.19"
libraryDependencies += "de.heikoseeberger" %% "akka-http-circe" % "1.25.2"