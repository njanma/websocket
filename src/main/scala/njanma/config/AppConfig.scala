package njanma.config

import pureconfig.generic.auto._

final case class AppConfig(db: DbConfig, server: ServerConfig)

final case class DbConfig(dataSource: String, user: String, password: String)

final case class ServerConfig(host: String, port: Int, connectionPath: String)
