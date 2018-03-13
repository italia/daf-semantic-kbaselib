package it.almawave.linkeddata.kb.parsers

import java.net.URL
import it.almawave.linkeddata.kb.catalog.OntologyBox
import it.almawave.linkeddata.kb.utils.JSONHelper
import it.almawave.linkeddata.kb.catalog.VocabularyBox

/*
 * ontology metadata extraction example
 *
 * TODO: provide a proper JUnit test
 */
object MainVocabularyBoxParser extends App {

  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/licences/licences.ttl")
  println(s"\n\nextracting informations from\n${url}\n...")

  val box = VocabularyBox.parse(url)
  box.start()

  val json = JSONHelper.writeToString(box.meta)
  println(json)

  println(box)
  println("triples: " + box.triples)

  box.stop()

}