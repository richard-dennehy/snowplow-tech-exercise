package controllers

import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.JsonSchemaFactory
import play.api.libs.json.{JsNull, JsObject, JsValue, Json}
import play.api.mvc._

import javax.inject.Inject
import scala.collection.mutable
import scala.jdk.CollectionConverters.IterableHasAsScala

class JsonSchemaController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  private val inMemorySchemas: mutable.Map[String, JsValue] = mutable.Map.empty
  private val schemaFactory = JsonSchemaFactory.byDefault()

  def uploadSchema(schemaId: String): Action[AnyContent] = Action { request =>
    val action = "uploadSchema"

    request.body.asJson match {
      case Some(json) =>
        inMemorySchemas(schemaId) = json
        Created(Json.obj(
          "action" -> action,
          "id" -> schemaId,
          "status" -> "success"
        ))
      case None => BadRequest(Json.obj(
        "action" -> action,
        "id" -> schemaId,
        "status" -> "error",
        "message" -> "Invalid JSON"
      ))
    }
  }

  def getSchema(schemaId: String): Action[AnyContent] = Action { _ =>
    val action = "getSchema"

    inMemorySchemas.get(schemaId) match {
      case Some(schema) =>
        Ok(Json.obj(
          "action" -> action,
          "id" -> schemaId,
          "status" -> "success",
          "schema" -> schema
        ))
      case None =>
        NotFound(Json.obj(
          "action" -> action,
          "id" -> schemaId,
          "status" -> "error",
          "message" -> "not found"
        ))
    }
  }

  def validate(schemaId: String): Action[AnyContent] = Action { request =>
    def trimNulls(json: JsValue): JsValue = {
      json match {
        case JsObject(underlying) =>
          JsObject(underlying.filter {
            case (_, JsNull) => false
            case _ => true
          })
        case _ => json
      }
    }
    val action = "validateDocument"

    request.body.asJson match {
      case Some(json) =>
        val trimmed = trimNulls(json)
        val payloadAsJackson = JsonLoader.fromString(trimmed.toString())
        inMemorySchemas.get(schemaId) match {
          case Some(schema) =>
            val schemaAsJackson = JsonLoader.fromString(schema.toString())
            val report = schemaFactory.getValidator.validate(schemaAsJackson, payloadAsJackson)
            if (report.isSuccess) {
              Ok(Json.obj(
                "action" -> action,
                "id" -> schemaId,
                "status" -> "success"
              ))
            } else {
              BadRequest(Json.obj(
                "action" -> action,
                "id" -> schemaId,
                "status" -> "error",
                "errors" -> report.asScala.map(_.getMessage)
              ))
            }
          case None =>
            NotFound(Json.obj(
              "action" -> action,
              "id" -> schemaId,
              "status" -> "error",
              "message" -> "schema not found"
            ))
        }
      case None =>
        BadRequest(Json.obj(
          "action" -> action,
          "id" -> schemaId,
          "status" -> "error",
          "message" -> "Invalid JSON"
        ))
    }
  }
}
