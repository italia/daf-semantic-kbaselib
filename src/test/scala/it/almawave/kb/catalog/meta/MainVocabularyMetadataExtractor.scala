package it.almawave.kb.catalog.meta

import utilities.JSONHelper
import java.net.URL

object MainVocabularyMetadataExtractor extends App {

  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master//VocabolariControllati/ClassificazioneTerritorio/Istat-Classificazione-08-Territorio.ttl")
  println(s"\n\nextracting informations from\n${url}\n...")

  val results = VocabularyMetadataExtractor(url)
  val json = JSONHelper.write(results)
  println(json)

}