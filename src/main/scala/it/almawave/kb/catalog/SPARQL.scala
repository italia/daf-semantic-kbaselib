package it.almawave.kb.catalog

import org.eclipse.rdf4j.repository.Repository
import scala.collection.mutable.ListBuffer
import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.query.QueryLanguage

object SPARQL {
  def apply(repo: Repository) = new SPARQL(repo)
}

/**
 *
 */
class SPARQL(repo: Repository) {

  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

  def query(query: String): Seq[Map[String, Any]] = {

    // TODO: handle UPDATE by Exception

    if (query.contains("SELECT ") || query.contains("select ")) {
      queryTuple(query)
    } else {
      queryGraph(query)
    }

  }

  def queryGraph(query: String): Seq[Map[String, Any]] = {
    //    val conn = repo.getConnection
    //    val statements = conn.prepareGraphQuery(QueryLanguage.SPARQL, query).evaluate()
    //    val results = new ListBuffer[Statement]
    //    while (statements.hasNext())
    //      results += statements.next()
    //    conn.close()
    throw new RuntimeException("GRAPH QUERY Not implemented yet!")
  }

  def queryTuple(query: String): Seq[Map[String, Any]] = {

    val conn = repo.getConnection

    val results = new ListBuffer[BindingSet]

    val tuples = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
    val binding_names = tuples.getBindingNames.toSet
    while (tuples.hasNext())
      results += tuples.next()

    conn.close()

    results.toStream
      .map { _.map { el => (el.getName, el.getValue.stringValue()) }.toMap }

  }

}
