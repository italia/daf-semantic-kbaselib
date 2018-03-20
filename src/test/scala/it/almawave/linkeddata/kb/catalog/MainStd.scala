package it.almawave.linkeddata.kb.catalog

import java.nio.file.Paths
import com.typesafe.config.ConfigFactory

object MainStd extends App {

  val conf = ConfigFactory.parseFile(Paths.get("src/test/resources/conf/catalog.conf").normalize().toFile())

  var catalog: CatalogBox = new CatalogBox(conf)
  catalog.start()

  catalog.vocabularies.foreach { v =>

    println()
    println(v)
    val vbox = catalog.resolveVocabularyDependencies(v)
    println(vbox)
  }

  catalog.stop()

}