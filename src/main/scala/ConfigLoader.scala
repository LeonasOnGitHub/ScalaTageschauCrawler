import com.typesafe.config.ConfigFactory

object ConfigLoader {
  val config = ConfigFactory.load()

  val mongoConfig = config.getConfig("mongodb")
  val host = mongoConfig.getString("host")
  val port = mongoConfig.getInt("port")
  val database = mongoConfig.getString("database")
  val name = mongoConfig.getString("name")
  val password = mongoConfig.getString("password")
}
