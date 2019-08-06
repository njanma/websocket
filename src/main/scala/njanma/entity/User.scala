package njanma.entity

case class User(username: String, passHash: String, userType: String)

object User {
  abstract sealed class Type(val alias: String)
  case object Admin extends Type("admin")

}
