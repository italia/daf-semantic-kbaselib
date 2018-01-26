package it.almawave.linkeddata.kb.catalog

import it.almawave.linkeddata.kb.catalog.models.OntologyMeta
import it.almawave.linkeddata.kb.catalog.models.VocabularyMeta
import it.almawave.linkeddata.kb.utils.JSONHelper

// TODO: create a JUnit for checking conventions!

object MainResourcesLoader extends App {

  val loader = ResourcesLoader("src/main/resources/conf/catalog.conf")

  val ontologies: Seq[OntologyMeta] = loader.fetchOntologies(false)
  val vocabularies: Seq[VocabularyMeta] = loader.fetchVocabularies(false)

  println("\n\nONTOLOGIES")
  ontologies.foreach { meta =>

    val source = loader.cacheFor(meta.source)
    println("SOURCE URL: " + source)

    val json = JSONHelper.writeToString(meta)
    println(json)
  }

  println("\n\nVOCABULARIES")
  vocabularies.foreach { meta =>
    val json = JSONHelper.writeToString(meta)
    println(json)
  }

}
