package controllers

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsNull, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FutureAwaits, Injecting}

import java.util.UUID

class JsonSchemaControllerITSpec extends PlaySpec with GuiceOneServerPerSuite with FutureAwaits with DefaultAwaitTimeout with Injecting {
  private val wsClient = inject[WSClient]
  private val baseUrl = s"http://localhost:$port"

  "POST /schema/:schemaId" should {
    "respond with 400 Bad Request and a JSON error message when the provided schema is not valid JSON" in {
      val schemaId = UUID.randomUUID().toString

      val response = await(wsClient.url(s"$baseUrl/schema/$schemaId").post("definitely not JSON"))
      response.status mustBe BAD_REQUEST
      response.json mustBe Json.obj(
        "action" -> "uploadSchema",
        "id" -> schemaId,
        "status" -> "error",
        "message" -> "Invalid JSON"
      )
    }

    "respond with 201 Created and the schema ID and status when the provided schema is valid JSON" in {
      val schemaId = UUID.randomUUID().toString

      val response = await(wsClient.url(s"$baseUrl/schema/$schemaId").post(basicSchema))
      response.status mustBe CREATED
      response.json mustBe Json.obj(
        "action" -> "uploadSchema",
        "id" -> schemaId,
        "status" -> "success",
      )
    }
  }

  "GET /schema/:schemaId" should {
    "respond with 404 Not Found when no schema exists with the provided schema ID" in {
      val schemaId = UUID.randomUUID().toString

      val response = await(wsClient.url(s"$baseUrl/schema/$schemaId").get())
      response.status mustBe NOT_FOUND
      response.json mustBe Json.obj(
        "action" -> "getSchema",
        "id" -> schemaId,
        "status" -> "error",
        "message" -> "not found"
      )
    }

    "respond with 200 OK and the schema when the schema ID exists" in {
      val schemaId = UUID.randomUUID().toString

      val uploadResponse = await(wsClient.url(s"$baseUrl/schema/$schemaId").post(basicSchema))
      uploadResponse.status mustBe CREATED

      val downloadResponse = await(wsClient.url(s"$baseUrl/schema/$schemaId").get())
      downloadResponse.status mustBe OK
      downloadResponse.json mustBe Json.obj(
        "action" -> "getSchema",
        "id" -> schemaId,
        "status" -> "success",
        "schema" -> basicSchema
      )
    }
  }

  "POST /validate/:schemaId" should {
    "respond with 404 Not Found when no schema exists with the provided schema ID" in {
      val schemaId = UUID.randomUUID().toString

      val response = await(wsClient.url(s"$baseUrl/validate/$schemaId").post(Json.obj("name" -> "X")))
      response.status mustBe NOT_FOUND
      response.json mustBe Json.obj(
        "action" -> "validateDocument",
        "id" -> schemaId,
        "status" -> "error",
        "message" -> "schema not found"
      )
    }

    "respond with 400 Bad Request and the validation errors as JSON when the provided JSON document does not match the schema" in {
      val schemaId = UUID.randomUUID().toString

      val uploadResponse = await(wsClient.url(s"$baseUrl/schema/$schemaId").post(basicSchema))
      uploadResponse.status mustBe CREATED

      val validateResponse = await(wsClient.url(s"$baseUrl/validate/$schemaId").post(Json.obj("not-name" -> "X")))
      validateResponse.status mustBe BAD_REQUEST
      validateResponse.json mustBe Json.obj(
        "action" -> "validateDocument",
        "id" -> schemaId,
        "status" -> "error",
        "errors" -> Json.arr(
          """object has missing required properties (["name"])"""
        )
      )
    }

    "respond with 400 Bad Request and an error message as JSON when the payload is not valid JSON" in {
      val schemaId = UUID.randomUUID().toString

      val response = await(wsClient.url(s"$baseUrl/validate/$schemaId").post("still not JSON"))
      response.status mustBe BAD_REQUEST
      response.json mustBe Json.obj(
        "action" -> "validateDocument",
        "id" -> schemaId,
        "status" -> "error",
        "message" -> "Invalid JSON"
      )
    }

    "respond with 200 OK and a success message as JSON when the provided JSON document matches the schema" in {
      val schemaId = UUID.randomUUID().toString

      val uploadResponse = await(wsClient.url(s"$baseUrl/schema/$schemaId").post(basicSchema))
      uploadResponse.status mustBe CREATED

      val validateResponse = await(wsClient.url(s"$baseUrl/validate/$schemaId").post(Json.obj("name" -> "X")))
      validateResponse.status mustBe OK
      validateResponse.json mustBe Json.obj(
        "action" -> "validateDocument",
        "id" -> schemaId,
        "status" -> "success",
      )
    }

    "strip null values from the provided JSON document" in {
      val schemaId = UUID.randomUUID().toString

      val uploadResponse = await(wsClient.url(s"$baseUrl/schema/$schemaId").post(onlyAllowsEmpty))
      uploadResponse.status mustBe CREATED

      val validateResponse = await(wsClient.url(s"$baseUrl/validate/$schemaId").post(Json.obj("name" -> JsNull)))
      validateResponse.json mustBe Json.obj(
        "action" -> "validateDocument",
        "id" -> schemaId,
        "status" -> "success",
      )
      validateResponse.status mustBe OK
    }

    "strip nested null values from the provided JSON document" in {
      val schemaId = UUID.randomUUID().toString

      val uploadResponse = await(wsClient.url(s"$baseUrl/schema/$schemaId").post(simplifiedConfigSchema))
      uploadResponse.status mustBe CREATED

      val document = Json.obj(
        "chunks" -> Json.obj(
          "size" -> 1024,
          "number" -> JsNull,
        )
      )
      val validateResponse = await(wsClient.url(s"$baseUrl/validate/$schemaId").post(document))
      validateResponse.json mustBe Json.obj(
        "action" -> "validateDocument",
        "id" -> schemaId,
        "status" -> "success",
      )
      validateResponse.status mustBe OK
    }
  }

  private lazy val basicSchema = Json.obj(
    "$schema" -> "http://json-schema.org/draft-04/schema#",
    "type" -> "object",
    "properties" -> Json.obj(
      "name" -> Json.obj("type" -> "string")
    ),
    "required" -> Json.arr("name")
  )

  private lazy val onlyAllowsEmpty = Json.obj(
    "$schema" -> "http://json-schema.org/draft-04/schema#",
    "type" -> "object",
    "additionalProperties" -> false
  )

  private lazy val simplifiedConfigSchema = Json.obj(
    "$schema" -> "http://json-schema.org/draft-04/schema#",
    "type" -> "object",
    "properties" -> Json.obj(
      "chunks" -> Json.obj(
        "type" -> "object",
        "properties" -> Json.obj(
          "size" -> Json.obj("type" -> "integer"),
          "number" -> Json.obj("type" -> "integer"),
        )
      )
    )
  )
}
