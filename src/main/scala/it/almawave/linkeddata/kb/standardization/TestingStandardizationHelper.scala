package it.almawave.linkeddata.kb.standardization

import java.nio.file.Paths
import it.almawave.linkeddata.kb.catalog.CatalogBox
import com.typesafe.config.ConfigFactory
import it.almawave.linkeddata.kb.catalog.SPARQL

object TestingStandardizationHelper extends App {

  val conf = ConfigFactory.parseFile(Paths.get("src/main/resources/conf/catalog.conf").normalize().toFile())
  val catalog = new CatalogBox(conf)
  catalog.start()

  val std = new StandardizationHelper(catalog)

  val vocID = "Licenze" //AccommodationTypology"
  val vbox = std.vocabularyWithDependency(vocID).get
  vbox.start()

  val cells = std.standardize_data(vbox) // TODO: introduce a model!

  // DEBUG
  cells.foreach(c => println(c))

  //  val deep = cells.toList.map(_.toList.size).max
  val MAX_LEVELS = std.max_levels(vbox)

  // DEBUG
  val keys = cells.toList.filter(_.toList.size == MAX_LEVELS)(0).flatMap(_.map(_._1))
  println("\n\nKEYS: " + keys.mkString(" | "))
  println("KEYS size: " + keys.size)

  vbox.stop()
  catalog.stop()
}