# neo4s
Neo4j Scala driver.

Features:

* Non-blocking
* Streaming
* Transactions

The goal of this driver is to allow you everything Neo4j transactional endpoint allows you to do, but in Scala.

# Setup
```
libraryDependencies += "ru.dgolubets" %% "neo4s" % "0.1.0"
```

# Simple query
Create a single driver and connection instance per application.
```
val driver = NeoDriver()
val connection = driver.connection("localhost", 7476)
```
Run query.
```
connection.run(cypher"""MATCH (p: Person) -[:ACTED_IN]-> (m: Movie) RETURN p, m.title LIMIT 2""").map { result =>
  result.data.flatMap(_.row).foreach {
    case CypherRow(CyObject(person), CyString(title)) =>
      println(s"${person("name").as[String]} acted in '$title'")
  }
}
```

# Query parameters
Neo4j encourages you to use parametrized queries, so it can cache query plans.

```
val year = 2008
val statementWithParameters = cypher"""MATCH (m: Movie) WHERE m.released > $year RETURN m"""
```

# Query graphs
```
connection.run(cypher"""MATCH (p: Person) -[r:ACTED_IN]-> (m: Movie) RETURN p, r, m LIMIT 2""".withGraph).map { result =>
	result.data.flatMap(_.graph).map { graph =>
	  graph.nodes.foreach { r =>
			println(r)
	  }
	  graph.relationships.foreach { r =>
			println(r)
	  }
	}
}
```

# Query stats
```
connection.run(cypher"""CREATE (m: Movie{ title: "Unknown", released: "2007"})""".withStats).map { result =>
  result.stats.map { stats =>
    println(s"Created ${stats.nodesCreated} nodes.")
  }
}
```

# Transactions
```
val t = connection.beginTransaction()
t.run(cypher"""CREATE (m: Movie{ title: "Unknown", released: "2007"})""".withStats).map { _ =>
  t.commit()
}
```

# Streaming
Neo4j results can be processed as a stream.
```
import driver.materializer
connection.stream(cypher"""MATCH (p: Person) -[:ACTED_IN]-> (m: Movie) RETURN p, m.title""").runForeach {
  case CypherStreamResultData(_, Some(row), _) => println(row)
  case CypherStreamError(code, message) =>
  case _ =>
}
```