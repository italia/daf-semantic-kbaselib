package it.almawave.kb.catalog

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.nio.file.Paths
import java.net.URL
import java.io.File
import com.typesafe.config.Config
import it.almawave.kb.catalog.models.OntologyMeta
import it.almawave.kb.catalog.models.VocabularyMeta
import utilities.JSONHelper

// TODO: create a JUnit for checking conventions!

object MainResourcesLoader extends App {

  //  val loader = ResourcesLoader("it/almawave/kb/catalog/catalog.conf")
  val loader = ResourcesLoader("./conf/catalog.conf")

  val ontologies: Seq[OntologyMeta] = loader.fetchOntologies(false)

  val vocabularies: Seq[VocabularyMeta] = loader.fetchVocabularies(false)

  println("\n\nONTOLOGIES")
  ontologies.foreach { meta =>

    val source = loader.cacheFor(meta.source)
    println("SOURCE URL: " + source)

    val json = JSONHelper.write(meta)
    println(json)
  }

  println("\n\nVOCABULARIES")
  vocabularies.foreach { meta =>
    val json = JSONHelper.write(meta)
    println(json)
  }

}
