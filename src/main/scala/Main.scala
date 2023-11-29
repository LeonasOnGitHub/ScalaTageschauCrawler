

import org.mongodb.scala._
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters._
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {
  //Parameters:
  val connectionString = s"mongodb://${ConfigLoader.name}:${ConfigLoader.password}@${ConfigLoader.host}:${ConfigLoader.port}/?authMechanism=SCRAM-SHA-256&authSource=${ConfigLoader.database}"
  val dbObject = new Mongo()

  //Exeption Logger
  val logger: Logger = LoggerFactory.getLogger(getClass)

  // MongoDB-Verbindungsdaten
  var mongoClient = dbObject.getMongoClient(connectionString)
  val database = dbObject.getDatabase(mongoClient, "Projektstudium")
  val collection: MongoCollection[BsonDocument] = database.getCollection("tagesschau_raw_data")


  // Crawler starten und ergebnis überprüfen
  val jsVal = Crawler.getTagesschauNewsPageApi("https://tagesschau.de/api2/news/")
  if (jsVal == JsNull || jsVal.toString == "{}") {
    logger.error("Crawler couldn't find any Data")
  }

  // JsValue in ein JsArray umwandeln
  val jsonArray: JsResult[JsArray] = jsVal.validate[JsArray]

  var insertedDocs=0
  var docAlreadyInDB=0
  // Überprüfen, ob die Validierung erfolgreich war
  jsonArray.fold(
    errors => {
      // Fehlerbehandlung, falls die Validierung fehlschlägt
      logger.error(s"error while parsing the JsValue: $errors")
    },
    jsArray => {
      // JsArray wurde erfolgreich extrahiert
      jsArray.value.foreach { jsEntry =>
        // Überprüfen, ob das Dokument bereits in der Collection vorhanden ist
        //Wandle den JsValue in ein BsonDoc um
        val bsonDocument: BsonDocument = BsonDocument.apply(Json.stringify(jsEntry))
        val title = bsonDocument.get("title")
        val date = bsonDocument.get("date")
        val existingDocumentObservable = collection.find(
          and(
            equal("title", title),
            equal("date", date)
          )
        ).limit(1)
        val existingDocument = Await.result(existingDocumentObservable.toFuture(), Duration.Inf)

        // falls das Dokument  noch nicht in der Collection vorhanden ist, füge es hinzu
        if (existingDocument.isEmpty) {
          //Schreibe den eintrag in die Datenbank
          val insertObservable = collection.insertOne(bsonDocument)
          Await.result(insertObservable.toFuture(), Duration.Inf)

          insertedDocs+=1
        } else {
          docAlreadyInDB+=1
        }

      }
    }
  )
  println("Inserted articles: " + insertedDocs)
  println("Articles already in database: " + docAlreadyInDB)

  // Schließe die Verbindung zur MongoDB
  mongoClient.close()
}
