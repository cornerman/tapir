package sttp.tapir.docs.openapi

import sttp.tapir._
import sttp.tapir.docs.apispec.schema.{SchemasForEndpoints, ToNamedSchemas}
import sttp.tapir.docs.apispec.{SecuritySchemesForEndpoints, nameAllPathCapturesInEndpoint}
import sttp.tapir.internal._
import sttp.tapir.openapi._

import scala.collection.immutable.ListMap

private[openapi] object EndpointToOpenAPIDocs {
  def toOpenAPI(
      api: Info,
      es: Iterable[EndpointWithDocsMetadata],
      options: OpenAPIDocsOptions,
      docsExtensions: List[DocsExtension[_]]
  ): OpenAPI = {
    val es2 = es
      .filter(e => findWebSocket(e.endpoint).isEmpty)
      .map(e => e.copy(endpoint = nameAllPathCapturesInEndpoint(e.endpoint)))
    val toNamedSchemas = new ToNamedSchemas
    val (keyToSchema, schemas) = new SchemasForEndpoints(es2.map(_.endpoint), options.schemaName, toNamedSchemas).apply()
    val securitySchemes = SecuritySchemesForEndpoints(es2.map(_.endpoint))
    val pathCreator = new EndpointToOpenAPIPaths(schemas, securitySchemes, options)
    val componentsCreator = new EndpointToOpenAPIComponents(keyToSchema, securitySchemes)

    val base = apiToOpenApi(api, componentsCreator, docsExtensions)

    es2.map(pathCreator.pathItem).foldLeft(base) { case (current, (path, pathItem)) =>
      current.addPathItem(path, pathItem)
    }
  }

  private def apiToOpenApi(info: Info, componentsCreator: EndpointToOpenAPIComponents, docsExtensions: List[DocsExtension[_]]): OpenAPI = {
    OpenAPI(
      info = info,
      tags = List.empty,
      servers = List.empty,
      paths = Paths(ListMap.empty),
      components = componentsCreator.components,
      security = List.empty,
      extensions = DocsExtensions.fromIterable(docsExtensions)
    )
  }
}
