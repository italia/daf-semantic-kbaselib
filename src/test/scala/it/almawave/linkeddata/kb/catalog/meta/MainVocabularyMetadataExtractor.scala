package it.almawave.linkeddata.kb.catalog.meta

import java.net.URL
import it.almawave.linkeddata.kb.parsers.meta.VocabularyMetadataExtractor
import it.almawave.linkeddata.kb.utils.JSONHelper

object MainVocabularyMetadataExtractor extends App {

  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master//VocabolariControllati/ClassificazioneTerritorio/Istat-Classificazione-08-Territorio.ttl")
  println(s"\n\nextracting informations from\n${url}\n...")

  val results = VocabularyMetadataExtractor(url)
  val json = JSONHelper.writeToString(results)
  println(json)

}