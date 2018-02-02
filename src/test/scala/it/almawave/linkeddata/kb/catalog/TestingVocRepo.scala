package it.almawave.linkeddata.kb.catalog

import it.almawave.linkeddata.kb.utils.ConfigHelper
import com.typesafe.config.ConfigFactory
import java.io.File
import it.almawave.linkeddata.kb.parsers.meta.OntologyMetadataExtractor
import it.almawave.linkeddata.kb.file.RDFFileRepository
import java.net.URL

object TestingVocRepo extends App {

  val source_url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/IndirizziLuoghi/latest/CLV-AP_IT.ttl")
  val onto_url = "http://dati.gov.it/onto/Istat-Classificazione-08-Territorio"

  val repo = new RDFFileRepository(source_url)
  if (!repo.isInitialized()) repo.initialize()

  val onto = OntologyMetadataExtractor(source_url)
//  onto.start()

  val results = onto.sparql.query("SELECT (COUNT(?s) AS ?triples) WHERE { ?s ?p ?o }")
    .toList(0).getOrElse("triples", 0)
  println("TRIPLES: " + results)

  val info = onto.informations()
  info.meta.concepts.foreach { concept => println(concept) }

//  onto.stop()

}