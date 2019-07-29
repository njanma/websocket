package njanma.config

import pureconfig.generic.auto._

case class DbConfig(db: DbConfigDetails)
case class DbConfigDetails(dataSource: String, user: String, password: String)
