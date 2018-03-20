package it.almawave.linkeddata.kb.catalog

import org.eclipse.rdf4j.repository.Repository
import scala.collection.mutable.ListBuffer
import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.query.QueryLanguage

object SPARQL {
  def apply(repo: Repository) = new SPARQL(repo)
}

/**
 * TODO: refactoring as a facade for extracting results into
 * + tuples: `Seq[Map[String, Any]]`
 * + graphs: `Seq[Map[String, Map[String, ...]]]`
 *
 * TODO: merge with managers/sparql
 *
 * should we assume that the repository is accessible (already initialized and not shutdown?)
 *
 */
class SPARQL(repo: Repository) {

  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

  def start() {
    if (!repo.isInitialized()) repo.initialize()
  }

  def stop() {
    if (repo.isInitialized()) repo.shutDown()
  }

  def ask(query: String): Boolean = {
    val conn = repo.getConnection
    val result = conn.prepareBooleanQuery(QueryLanguage.SPARQL, query).evaluate()
    conn.close()
    result
  }

  def query(query: String): Seq[Map[String, Any]] = {

    // TODO: handle UPDATE by Exception

    if (query.contains("SELECT ") || query.contains("select ")) {
      queryTuple(query)
    } else if (query.contains("CONSTRUCT ") || query.contains("construct ") || query.contains("DESCRIBE ") || query.contains("describe ")) {
      queryGraph(query)
    } else {
      throw new RuntimeException("cannot handle this type of query")
    }

  }

  // TODO
  def queryGraph(query: String): Seq[Map[String, Any]] = {
    //    val conn = repo.getConnection
    //    val statements = conn.prepareGraphQuery(QueryLanguage.SPARQL, query).evaluate()
    //    val results = new ListBuffer[Statement]
    //    while (statements.hasNext())
    //      results += statements.next()
    //    conn.close()
    throw new RuntimeException("GRAPH QUERY Not implemented yet!")
  }

  // REFACTORIZE
  def queryTuple(query: String): Seq[Map[String, Any]] = {

    if (!repo.isInitialized()) repo.initialize() // VERIFY if needed
    val conn = repo.getConnection

    val results = new ListBuffer[BindingSet]

    val tuples = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()
    val binding_names = tuples.getBindingNames.toSet

    while (tuples.hasNext()) {

      val tuple: BindingSet = tuples.next()

      results += tuple

    }

    conn.close()

    // projection of the results in terms of a stream
    results.toStream
      .map { _.map { el => (el.getName, el.getValue.stringValue()) }.toMap } // TODO: avoid String using typed values!

  }

}
