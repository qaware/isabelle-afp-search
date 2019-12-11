package de.qaware.findfacts.webapp.controllers

import com.typesafe.scalalogging.Logger
import de.qaware.findfacts.common.dt.{BaseEt, EtField}
import de.qaware.findfacts.core.{FacetQuery, Filter, FilterQuery, Id, Query, QueryService}
import de.qaware.findfacts.webapp.utils.JsonUrlCodec
// scalastyle:off
import io.circe.generic.auto._
import io.circe.syntax._
// scalastyle:on
import io.circe.{Json, Printer}
import io.swagger.annotations.{Api, ApiImplicitParam, ApiImplicitParams, ApiOperation, ApiParam, ApiResponse, ApiResponses, Example, ExampleProperty}
import play.api.libs.circe.Circe
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request, Result}

import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/** Controller for the query api.
  *
  * @param cc injected components
  */
@Api(value = "/")
class QueryController(cc: ControllerComponents, queryService: QueryService, urlCodec: JsonUrlCodec)
    extends AbstractController(cc)
    with Circe {
  private final val InternalErrorMsg = "Internal server error"
  private final val NotFoundMsg = "Entity not found"

  private val logger = Logger[QueryController]

  // Json printer
  implicit val jsonPrinter: Printer = Printer.noSpacesSortKeys.copy(dropNullValues = true)

  protected def executeQuery(query: Query): Result = {
    val json: Try[Json] = query match {
      case query: FilterQuery => queryService.getResults(query).map(_.toList.asJson)
      case query @ FacetQuery(_, field) =>
        import field.keyEncoder
        queryService.getFacetResults[field.BaseType](query).map(_.asJson)
    }
    json match {
      case Failure(exception) =>
        logger.error(s"Error executing query $query", exception)
        InternalServerError(InternalErrorMsg)
      case Success(value) => Ok(value)
    }
  }

  @ApiOperation(
    value = "Search query",
    notes = "Accepts a search query and returns list of all results.",
    response = classOf[BaseEt],
    responseContainer = "List",
    httpMethod = "POST"
  )
  @ApiResponses(Array(new ApiResponse(code = 400, message = "Invalid Query")))
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "query",
        value = "Query object",
        required = true,
        paramType = "body",
        dataType = "de.qaware.findfacts.core.Query",
        examples = new Example(value = Array(new ExampleProperty(
          mediaType = "default",
          value = """{
  "FilterQuery": {
    "filter": {
      "Filter": {
        "fieldTerms": {
          "Name": {
            "StringExpression": {
              "inner": "*gauss*"
            }
          }
        }
      }
    },
    "maxResults": 10
  }
}"""
        )))
      )))
  def search: Action[Query] = Action(circe.json[Query]) { implicit request: Request[Query] =>
    executeQuery(request.body)
  }

  @ApiOperation(
    value = "Encode query as url",
    notes = "Encodes query as url for permalinks.",
    response = classOf[String],
    httpMethod = "POST"
  )
  @ApiResponses(Array(new ApiResponse(code = 400, message = "Invalid Query")))
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        name = "query",
        value = "Query object",
        required = true,
        paramType = "body",
        dataType = "de.qaware.findfacts.core.Query",
        examples = new Example(value = Array(new ExampleProperty(
          mediaType = "default",
          value = """{
  "FacetQuery" : {
    "filter" : {
      "Filter" : {
        "fieldTerms" : {
          "Name" : {
            "StringExpression" : {
              "inner" : "*gauss*"
            }
          }
        }
      }
    },
    "field" : {
      "Kind" : {}
    }
  }
}"""
        )))
      )))
  def encode: Action[Query] = Action(circe.json[Query]) { implicit request: Request[Query] =>
    // Parse back into json - parsing into query first does validation.
    Ok(urlCodec.encodeCompressed(request.body.asJson))
  }

  @ApiOperation(
    value = "Executes url-encoded query",
    notes = "Decodes query-url and executes query",
    response = classOf[BaseEt],
    responseContainer = "List",
    httpMethod = "GET"
  )
  def searchEncoded(@ApiParam(value = "Encoded query", required = true) encodedQuery: String): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      urlCodec.decodeCompressed(encodedQuery) match {
        case Failure(ex) =>
          logger.warn("Could not decode query string: ", ex)
          BadRequest("Corrupt query encoding")
        case Success(queryJson) =>
          queryJson.as[Query] match {
            case Left(value) => BadRequest(value.message)
            case Right(query) => executeQuery(query)
          }
      }
    }

  @ApiOperation(
    value = "Retrieves a single entity",
    notes = "Retrieves information about a single entity",
    response = classOf[BaseEt],
    httpMethod = "GET")
  @ApiResponses(Array(new ApiResponse(code = 400, message = NotFoundMsg)))
  def entity(@ApiParam(value = "ID of result entity to fetch", required = true) id: String): Action[AnyContent] =
    Action { implicit request: Request[AnyContent] =>
      val singleQuery = FilterQuery(Filter(Map(EtField.Id -> Id(id))), 2)
      queryService.getResults(singleQuery) match {
        case Failure(exception) =>
          logger.error(s"Error executing id query: $exception")
          InternalServerError(InternalErrorMsg)
        case Success(Vector(single)) => Ok(single.asJson)
        case Success(values) =>
          logger.error(s"Did not receive single value for id $id: ${values.mkString(",")}")
          BadRequest(NotFoundMsg)
      }
    }
}
