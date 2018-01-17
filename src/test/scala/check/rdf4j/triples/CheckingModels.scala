package check.rdf4j.triples

/**
 * This is a simple example for handling triples in scala idiomatic way
 */

case class Triple(
  sub: String,
  prp: String,
  obj: Object,
  contexts: Seq[String])

case class Graph(
  name: String,
  triples: Stream[Triple])
  
