package it.almawave.linkeddata.kb.parsers

import java.net.URL
import it.almawave.linkeddata.kb.catalog.OntologyBox
import it.almawave.linkeddata.kb.utils.JSONHelper

/*
 * ontology metadata extraction example
 *
 * TODO: provide a proper JUnit test
 */
object MainOntologyBoxParser extends App {

  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/Organizzazioni/latest/COV-AP_IT.ttl")
  println(s"\n\nextracting informations from\n${url}\n...")

  val box = OntologyBox.parse(url)
  box.start()
  println(box)

  val json = JSONHelper.writeToString(box.meta)
  println(json)

  box.stop()

}