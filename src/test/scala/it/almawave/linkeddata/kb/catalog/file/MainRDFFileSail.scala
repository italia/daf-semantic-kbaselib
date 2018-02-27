package it.almawave.linkeddata.kb.catalog.file

import java.net.URL

import org.eclipse.rdf4j.repository.sail.SailRepository

import it.almawave.linkeddata.kb.catalog.SPARQL
import it.almawave.linkeddata.kb.file.RDFFileSail;

object MainRDFFileSail extends App {

  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/Organizzazioni/latest/COV-AP_IT.ttl")
  val rdf_files = new RDFFileSail(List(url))

  val repo = new SailRepository(rdf_files)
  repo.initialize()

  val qu = SPARQL(repo)

  val results = qu.query("""
    SELECT DISTINCT ?concept 
    WHERE { ?sub a ?concept . }
    ORDER BY ?concept 
  """)

  results.toList.foreach { item =>
    println(item)
  }

  repo.shutDown()

}
