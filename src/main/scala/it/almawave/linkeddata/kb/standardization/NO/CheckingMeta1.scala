package it.almawave.linkeddata.kb.standardization.NO

import it.almawave.linkeddata.kb.catalog.CatalogBox
import it.almawave.linkeddata.kb.catalog.SPARQL
import it.almawave.linkeddata.kb.standardization.StandardizationHelper
import com.typesafe.config.ConfigFactory
import java.nio.file.Paths
import it.almawave.linkeddata.kb.utils.JSONHelper

object CheckingMeta1 extends App {

  val conf = ConfigFactory.parseFile(Paths.get("src/main/resources/conf/catalog.conf").normalize().toFile())
  val catalog = new CatalogBox(conf)
  catalog.start()

  val std = new StandardizationHelper(catalog)

  val vocID = "AccommodationTypology"
  val vbox = std.vocabularyWithDependency(vocID).get
  vbox.start()

  println("\n\nMETA 1 TEST")
  def meta1(ontologyID: String = "SKOS") = {
    SPARQL(vbox.repo).query("""
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      PREFIX clvapit: <http://dati.gov.it/onto/clvapit#>
      SELECT DISTINCT ?ontology ?concept ?prp ?ontologyID ?conceptID
      WHERE {
        ?uri a ?concept .
        ?uri ?prp [] .
        ?concept rdfs:isDefinedBy ?ontology .
      }    
    """)
      .toList
//      .map { _.toList.map { el => el._2.toString().replaceAll(".*[#/](.*)", "$1") } }
  }

  println(JSONHelper.writeToString(meta1()))

  vbox.stop()
  catalog.stop()

}