
import Main.getClass
import org.jsoup.Jsoup
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsArray, JsValue, Json, Writes}

case class TagesschauData(source: String, title: String, text: String, category: String, date: String, url: String)

object Crawler {

  val logger: Logger = LoggerFactory.getLogger(getClass)
  // Define an implicit Writes for TagesschauData
  implicit val tagesschauDataWrites: Writes[TagesschauData] = Json.writes[TagesschauData]
  val ENTRYURL = "https://tagesschau.de/api2/news/"

  def getTagesschauNewsPageApi(entryUrl: String): JsValue = {
    logger.info("Crawler started")
    val response: requests.Response = requests.get(entryUrl)
    val jsonInput = Json.parse(response.text)

    // Extract the "news" array from the JSON
    val newsArray = (jsonInput \ "news").as[JsArray]

    if (newsArray.value.nonEmpty) {
      // Iterate over each news item and create JSON objects
      val newsObjects = newsArray.value.map { newsItem =>
        val quelle = "Tagesschau"
        val title = (newsItem \ "title").as[String]
        val text = extractText((newsItem \ "detailsweb").asOpt[String].getOrElse(""))
        val kategorie = (newsItem \ "topline").asOpt[String].getOrElse("")
        val date = (newsItem \ "date").as[String]
        val url = (newsItem \ "detailsweb").asOpt[String].getOrElse("")

        TagesschauData(quelle, title, text, kategorie, date, url)
      }

      // Convert the list of TagesschauData objects to a JSON array
      Json.toJson(newsObjects)

    } else {
      throw new Exception("No news data found in the JSON response.")
    }

  }

  def extractText(url: String): String = {
    val defaultString = ""
    if (url == "") defaultString
    else {
      val doc = Jsoup.connect(url).get()
      val scriptElement = doc.select("script[type=application/ld+json]").first()
      val scriptContent = scriptElement.html()
      val json: JsValue = Json.parse(scriptContent)
      val articleBody: Option[String] = (json \ "articleBody").asOpt[String]
      articleBody.getOrElse(defaultString)
    }

  }


}