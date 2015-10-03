package ru.dgolubets.neo4s

/**
 * Credentials for connecting to Neo4j.
 * @param username username
 * @param password password
 */
case class NeoCredentials(username: String, password: String)
