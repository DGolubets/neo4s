package ru.dgolubets.neo4s.internal.json

import play.api.libs.json.Json
import ru.dgolubets.neo4s.internal.json
import ru.dgolubets.neo4s.model.{CypherQueryResponse, CypherQuery, CypherStatement}

class JsonSerializer {
  import json.ImplicitFormats._

  def formatRequest(query: CypherQuery) = Json.toJson(query)

  def parseResponse(json: String): CypherQueryResponse = Json.fromJson[CypherQueryResponse](Json.parse(json)).get
}