package it.almawave.linkeddata.kb.standardization

import it.almawave.linkeddata.kb.catalog.CatalogBox
import com.typesafe.config.ConfigFactory
import java.nio.file.Paths
import it.almawave.linkeddata.kb.catalog.VocabularyBox
import it.almawave.linkeddata.kb.catalog.SPARQL
import java.util.Map
import java.util.Collections

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import it.almawave.linkeddata.kb.utils.JSONHelper

object CheckingSPARQLWithStandard extends App {

  val conf = ConfigFactory.parseFile(Paths.get("src/main/resources/conf/catalog.conf").normalize().toFile())
  val catalog = new CatalogBox(conf)
  catalog.start()

  val std = new StdQuery(catalog)

  val vocID = "AccommodationTypology" // CHECK Licenze

  val vbox: VocabularyBox = std.vocabularyWithDependency(vocID).get
  vbox.start()

  //  val vbox_onto = "SKOS" // TODO: da configurare per vocablario!

  val ontologyID = vbox.extract_assetType()._1

  vbox.meta.instances
    .slice(11, 12)
    .foreach { el =>
      println("element: " + el)

      get_properties(el).map { el =>

        // LEVEL 1
        // simulazione della query, con concept dell'istanza e property
        val conceptID = el.getOrElse("concept", "").toString().replaceAll(".*[#/](.*)", "$1")
        val propertyID = el.getOrElse("prp", "").toString().replaceAll(".*[#/](.*)", "$1")
        val level_1 = s"${ontologyID}.${conceptID}.${propertyID}"

        println()
        println(el)
        println(level_1)

        // LEVEL 2
        // vbox.id
        // TODO: extract level

      }

    }

  def get_properties(uri: String) = {

    // TODO: DESCRIBE

    SPARQL(vbox.repo).query(s"""
      PREFIX adms: <http://www.w3.org/ns/adms#>
      SELECT ?concept ?prp 
      WHERE {
        <${uri}> a ?concept .
        <${uri}> ?prp [] .
      }  
    """).toList.distinct

  }

  // TODO: instances
  // IDEA: extract from [] adms:representationTechnique ?representation .

  vbox.stop()
  catalog.stop()

  // -------------------------------------------------------------------------------

  // TODO: find a way to produce good prefixes from ontologies
  def prefixes() = {
    """
    PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
    PREFIX clvapit: <http://dati.gov.it/onto/clvapit#>  
    """
  }

}


/* EXAMPLE SKOS
<http://dati.gov.it/onto/controlledvocabulary/AccoTypology/B141>
  a  	skos:Concept , accoapit:AccommodationTypology ;
  skos:inScheme <http://dati.gov.it/onto/controlledvocabulary/AccoTypology> ;
  skos:notation "B.1.4.1" ;
  dct:identifier "B.1.4.1" ;
  clvapit:hasRankOrder "4" ;
  skos:broader <http://dati.gov.it/onto/controlledvocabulary/AccoTypology/B14> ;
  skos:broaderTransitive <http://dati.gov.it/onto/controlledvocabulary/AccoTypology/B1> ;
  skos:prefLabel "Casa per ferie"@it ;
  skos:altLabel "Case per ferie"@it ;
  skos:prefLabel "Holiday House"@en ;
  skos:definition "Le case per ferie..."@it .
*/
