name := "websocket"

version := "0.1"

scalaVersion := "2.12.8"

scalacOptions ++= Seq("-Ypartial-unification", "-language:experimental.macros")

resolvers += Resolver.sonatypeRepo("releases")

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
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-generic-extras"
).map(_ % circeVersion)

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.8"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.19"
libraryDependencies += "de.heikoseeberger" %% "akka-http-circe" % "1.25.2"
libraryDependencies += "com.github.pureconfig" %% "pureconfig" % "0.11.1"

addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
)
