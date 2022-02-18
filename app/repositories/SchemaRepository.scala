package repositories

import play.api.libs.json.{JsObject, JsValue, Json}
import play.modules.reactivemongo.ReactiveMongoApi

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import reactivemongo.play.json.collection.JSONCollection

class SchemaRepository @Inject()(mongo: ReactiveMongoApi)(implicit ec: ExecutionContext) {
  private val collection = mongo.database.map(_.collection[JSONCollection]("schemas"))

  def insert(schemaId: String, schema: JsValue): Future[Unit] = {
    val document = Json.obj(
      "_id" -> schemaId,
      "schema" -> schema,
    )
    collection.flatMap {
      _.insert(ordered = false)
        .one(document)
        .map(_ => ())
    }
  }

  def get(schemaId: String): Future[Option[JsValue]] = {
    collection.flatMap {
      _.find(Json.obj("_id" -> schemaId))
        .one[JsObject]
        .map(_.flatMap(document => (document \ "schema").toOption))
    }
  }
}
