package it.almawave.kb.catalog.federation

import org.eclipse.rdf4j.sail.federation.Federation
import org.eclipse.rdf4j.repository.sail.SailRepository
import java.net.URL
import org.eclipse.rdf4j.query.QueryLanguage
import scala.collection.mutable.ListBuffer
import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.repository.Repository
import it.almawave.kb.catalog.file.RDFFileSail
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository
import org.eclipse.rdf4j.query.TupleQueryResultHandler
import it.almawave.kb.catalog.file.RDFQueryResultHandler
import org.eclipse.rdf4j.sail.lucene.LuceneSail

/**
 * this is a small POC to show how to use federation by convention over a collection of self-contained repository
 */
object TestingFederationFiles extends App {

  // CHECK: repository manager

  val urls = List(
    "https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/ClassificazioneCategoriePuntoInteresse/POICategoryClassification.ttl",
    "https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/IndirizziLuoghi/latest/CLV-AP_IT.ttl",
    "https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/PuntoDiInteresse/latest/POI-AP_IT.ttl",
    "https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/Livello0/latest/l0.ttl")

  val federation = new Federation
  federation.setReadOnly(true)
  federation.setDistinct(true)

  urls.foreach { url =>
    val context = "http://dati.gov.it/examples/" + url.toString().replaceAll(".*:/.*[#/](.*)\\..*", "$1")
    val urlSail = new RDFFileSail(new URL(url), context)
    federation.addMember(new SailRepository(urlSail))
  }

  //    federation.addMember(new SPARQLRepository("http://localhost:9999/blazegraph/sparql"))
  //  try {
  //    federation.addMember(new SPARQLRepository("http://localhost:8899/sparql"))
  //    //  concept=_:node1c0ooj3pkx40
  //  } catch {
  //    case err: Throwable => err.printStackTrace()
  //  }

  val repo = new SailRepository(federation)
  repo.initialize()

  val conn = repo.getConnection

  val handler = new RDFQueryResultHandler

  val tuples = conn.prepareTupleQuery(QueryLanguage.SPARQL, """
    
    SELECT DISTINCT ?graph ?concept 
    
    WHERE {
      GRAPH ?graph {
        ?concept a owl:Class .
        FILTER(!isBlank(?concept))
      }
    }
    
    ORDER BY ?graph ?concept 
    # LIMIT 100
    
  """).evaluate(handler)

  handler.toStream.foreach { item => println(item) }

  conn.close()

  repo.shutDown()

}