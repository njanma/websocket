name := "websocket"

version := "0.1"

scalaVersion := "2.12.8"

scalacOptions ++= Seq("-Ypartial-unification", "-language:experimental.macros")

resolvers += Resolver.sonatypeRepo("releases")
resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"

lazy val circeVersion = "0.10.0"
lazy val akkaHttpCirceVersion = "1.25.2"
lazy val doobieVersion = "0.6.0"
lazy val akkaHttpVersion = "10.1.9"
lazy val flywayVersion = "5.2.4"
lazy val akkaStreamVersion = "2.5.23"
lazy val pureconfigVersion = "0.11.1"
lazy val tsecV = "0.0.1-M11"
lazy val scalatestVersion = "3.0.8"
lazy val scalamockVersion = "4.1.0"
lazy val akkaStreamTestkitVersion = "2.5.23"

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
libraryDependencies ++= Seq(
  "io.github.jmcardon" %% "tsec-common",
  "io.github.jmcardon" %% "tsec-hash-jca"
).map(_ % tsecV)
libraryDependencies += "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaStreamVersion
libraryDependencies += "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirceVersion
libraryDependencies += "com.github.pureconfig" %% "pureconfig" % pureconfigVersion
libraryDependencies += "org.flywaydb" % "flyway-core" % flywayVersion

libraryDependencies += "org.scalactic" %% "scalactic" % scalatestVersion
libraryDependencies += "org.scalatest" %% "scalatest" % scalatestVersion % Test
libraryDependencies += "org.scalamock" %% "scalamock" % scalamockVersion % Test
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaStreamTestkitVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion
)

addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
)

addSbtPlugin(
  "com.artima.supersafe" % "sbtplugin" % "1.1.7"
)