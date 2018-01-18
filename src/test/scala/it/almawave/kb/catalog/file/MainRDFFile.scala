package it.almawave.kb.catalog.file

import java.net.URL

import org.eclipse.rdf4j.repository.sail.SailRepository

import it.almawave.kb.catalog.SPARQL

object MainRDFSail extends App {

  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/Organizzazioni/latest/COV-AP_IT.ttl")
  val rdf_files = new RDFFileSail(url)

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
