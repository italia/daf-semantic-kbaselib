package it.almawave.linkeddata.kb.parsers

import java.net.URL
import it.almawave.linkeddata.kb.catalog.OntologyBox
import it.almawave.linkeddata.kb.utils.JSONHelper
import it.almawave.linkeddata.kb.catalog.VocabularyBox

/*
 * ontology metadata extraction example
 */
object MainVocabularyBoxParser extends App {

  val url = new URL("file:///C:/Users/Al.Serafini/repos/DAF/daf-ontologie-vocabolari-controllati/VocabolariControllati/Licenze/Licenze.ttl")
  println(s"\n\nextracting informations from\n${url}\n...")

  val box = VocabularyBox.parse(url)
  println(box)

  val json = JSONHelper.writeToString(box.meta)
  println(json)

}