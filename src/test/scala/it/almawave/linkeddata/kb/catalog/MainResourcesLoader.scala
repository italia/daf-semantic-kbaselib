package it.almawave.linkeddata.kb.catalog

import it.almawave.linkeddata.kb.catalog.models.OntologyMeta
import it.almawave.linkeddata.kb.utils.JSONHelper
import it.almawave.linkeddata.kb.catalog.models.VocabularyMeta

// TODO: create a JUnit for checking conventions!

object MainResourcesLoader extends App {

  //  val loader = ResourcesLoader("./src/main/resources/config_examples/catalog.example")

  val loader = ResourcesLoader("C:/Users/Al.Serafini/repos/DAF/kataLOD/conf/catalog.conf")

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

  // TODO: port the OntologyRepository logic to vocabulary, refactorize!

}
