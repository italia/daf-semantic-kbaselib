package it.almawave.linkeddata.kb.federation

import org.eclipse.rdf4j.sail.federation.Federation
import org.eclipse.rdf4j.repository.sail.SailRepository
import java.net.URL
import org.eclipse.rdf4j.query.QueryLanguage
import scala.collection.mutable.ListBuffer
import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository
import org.eclipse.rdf4j.query.TupleQueryResultHandler
import org.eclipse.rdf4j.sail.lucene.LuceneSail
import it.almawave.linkeddata.kb.file.RDFFileSail
import it.almawave.linkeddata.kb.catalog.file.RDFQueryResultHandler

/**
 * this is a small POC to show how to use federation by convention over a collection of self-contained repository,
 * using also a remote SPARQL endpoint.
 *
 * NOTE:
 * 	using a combination of `Federation.setDistinct` and `SPARQLRepository.enableQuadMode` it's possible
 * 	to obtain multiple copies of the same URI, taken from different (local or remote) context.
 *
 * TODO:
 * 	we should benchmark the queries, in case we could improve performances when avoiding duplication
 * (for example forcing the usage of local copies as a "cache")
 *
 */
object TestingFederationFiles extends App {

  // CHECK: repository manager

  val urls = List(
    "https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/poi-category-classification/poi-category-classification.ttl",
    "https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/IndirizziLuoghi/latest/CLV-AP_IT.ttl",
    "https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/PuntoDiInteresse/latest/POI-AP_IT.ttl",
    "https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/l0/latest/l0-AP_IT.ttl")
    .map(new URL(_))

  val federation = new Federation
  federation.setReadOnly(true)
  federation.setDistinct(true)

  urls.foreach { url =>
    val context = "http://dati.gov.it/examples/" + url.toString().replaceAll(".*:/.*[#/](.*)\\..*", "$1")
    val urlSail = new RDFFileSail(List(url), context)
    federation.addMember(new SailRepository(urlSail))
  }

  //  TESTING with blazegraph (SPARQLRepository)
  val sparql_repo = new SPARQLRepository("http://localhost:9999/blazegraph/sparql")
  sparql_repo.enableQuadMode(true)
  federation.addMember(sparql_repo)

  val repo = new SailRepository(federation)
  repo.initialize()

  val conn = repo.getConnection

  val handler = new RDFQueryResultHandler

  val tuples = conn.prepareTupleQuery(QueryLanguage.SPARQL, """
    
    SELECT DISTINCT ?graph ?concept 
    
    WHERE {
      {
        GRAPH ?graph {
          ?concept a owl:Class .
          FILTER(!isBlank(?concept))
        }
		    FILTER(BOUND(?graph))
      }
      UNION 
      {
        ?concept a owl:Class .
        FILTER(!isBlank(?concept))
        BIND(<graph://default> AS ?graph)
      }
    }
    
    ORDER BY ?graph ?concept 
    
  """).evaluate(handler)

  // as an example, here we can get a list of concetxt for each published concept
  handler
    .toStream
    .toList
    .map { item => (item.getBinding("concept").getValue, item.getBinding("graph").getValue) }
    .toList
    .groupBy { _._1 }
    .map { item => (item._1, item._2.map(_._2).toList) }
    .foreach { item =>
      println(s"${item._1}\n\tIN [ ${item._2.mkString(" | ")} ]")
    }

  conn.close()

  repo.shutDown()

}