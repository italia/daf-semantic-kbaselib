package check.rdf4j.standardization

import it.almawave.linkeddata.kb.catalog.ResourcesLoader
import it.almawave.linkeddata.kb.catalog.models.OntologyMeta

object CheckMetadataLevels extends App {

  val loader = ResourcesLoader("src/main/resources/conf/catalog.conf")

  val list = loader.fetchOntologies(false)
    .map { onto => (onto.id, onto.concepts.toList.map(_.replaceAll("^.*[#/](.*)$", "$1"))) }

  list.foreach { item =>
    println("\n\nONTOLOGY: " + item._1)

    println(item._2.mkString(" | "))
  }

}