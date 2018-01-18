package it.almawave.kb.catalog.meta

import utilities.JSONHelper
import java.net.URL

object MainOntologyMetadataExtractor extends App {

  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/Organizzazioni/latest/COV-AP_IT.ttl")
  println(s"\n\nextracting informations from\n${url}\n...")

  val results = OntologyMetadataExtractor(url)
  println(results)

  val json = JSONHelper.write(results)
  println(json)

}