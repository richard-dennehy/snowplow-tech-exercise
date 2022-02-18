package controllers

import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.JsonSchemaFactory
import play.api.libs.json.{JsNull, JsObject, JsValue, Json}
import play.api.mvc._
import repositories.SchemaRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.IterableHasAsScala

class JsonSchemaController @Inject()(
  val controllerComponents: ControllerComponents,
  repository: SchemaRepository,
)(implicit ec: ExecutionContext) extends BaseController {
  private val schemaFactory = JsonSchemaFactory.byDefault()

  def uploadSchema(schemaId: String): Action[AnyContent] = Action.async { request =>
    val action = "uploadSchema"

    request.body.asJson match {
      case Some(json) =>
        repository.insert(schemaId, json).map { _ =>
          Created(Json.obj(
            "action" -> action,
            "id" -> schemaId,
            "status" -> "success"
          ))
        }
      case None => Future.successful(BadRequest(Json.obj(
        "action" -> action,
        "id" -> schemaId,
        "status" -> "error",
        "message" -> "Invalid JSON"
      )))
    }
  }

  def getSchema(schemaId: String): Action[AnyContent] = Action.async { _ =>
    val action = "getSchema"

    repository.get(schemaId) map {
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

  def validate(schemaId: String): Action[AnyContent] = Action.async { request =>
    def trimNulls(json: JsValue): JsValue = {
      json match {
        case JsObject(underlying) =>
          JsObject(underlying.flatMap {
            case (_, JsNull) =>
              None
            case (k, v) =>
              Some(k -> trimNulls(v))
          })
        case _ => json
      }
    }
    val action = "validateDocument"

    request.body.asJson match {
      case Some(json) =>
        val trimmed = trimNulls(json)
        val payloadAsJackson = JsonLoader.fromString(trimmed.toString())
        repository.get(schemaId) map {
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
        Future.successful(BadRequest(Json.obj(
          "action" -> action,
          "id" -> schemaId,
          "status" -> "error",
          "message" -> "Invalid JSON"
        )))
    }
  }
}
