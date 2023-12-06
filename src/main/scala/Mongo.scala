import org.mongodb.scala.{MongoClient, MongoDatabase}
import org.slf4j.{Logger, LoggerFactory}

class Mongo {

  //Exeption Logger
  val logger= LoggerFactory.getLogger(getClass)

  def getMongoClient(connectionString: String): MongoClient = {
    try {
      MongoClient(connectionString)
    } catch {
      case e: Exception =>
        logger.error("an exception occurred while trying to connect to MongoClient: ", e)
        //TODO \|/ kein leeren clinet senden
        MongoClient("")
    }
  }

  def getDatabase(mongoClient: MongoClient, db: String): MongoDatabase = {
    try {
      mongoClient.getDatabase(db)
    } catch {
      case e: Exception =>
        logger.error("an exception occurred while trying to connect to the Database: ", e)
        //TODO \|/ keine leere db senden
        mongoClient.getDatabase("")
    }
  }

}
