package it.almawave.linkeddata.kb.standardization

import java.nio.file.Paths
import com.typesafe.config.ConfigFactory
import it.almawave.linkeddata.kb.catalog.CatalogBox
import scala.collection.mutable.ListBuffer
import it.almawave.linkeddata.kb.catalog.OntologyBox
import java.net.URL
import it.almawave.linkeddata.kb.catalog.VocabularyBox
import org.slf4j.LoggerFactory
import it.almawave.linkeddata.kb.catalog.SPARQL
import it.almawave.linkeddata.kb.utils.JSONHelper
import scala.util.Try
import scala.collection.immutable.Nil

object CheckingQueriesForStandardization extends App {

  val conf = ConfigFactory.parseFile(Paths.get("src/main/resources/conf/catalog.conf").normalize().toFile())
  val catalog = new CatalogBox(conf)
  catalog.start()

  val std = new StdQuery(catalog)

  val vocID = "Licenze" //"AccommodationTypology" // CHECK Licenze

  println("\n\n\n\n#### RESOLVING DEPENDENCIES for VOCABULARIES...")
  val vbox: VocabularyBox = std.vocabularyWithDependency(vocID).get

  vbox.start()

  // EXTRACT level 1 for each item: from ontologyID, uri, {prp}_uri
  // EXTRACT level 2 for each item: from vocabularyID, level

  // CHECK ERROR / TYPO
  // (List(http://dati.gov.it/onto/controlledvocabulary/AccoTypology/D411, http://dati.gov.it/onto/controlledvocabulary/AccoTypology/D41),4,http://dati.gov.it/onto/controlledvocabulary/AccoTypology/D411)
  // TODO: verify rows with size < 4

  // extracting levels
  val MAX_LEVELS = std.extract_levels(vbox)
  println("MAX_LEVELS: " + MAX_LEVELS)

  std.extract_hierarchy(vbox)
    .groupBy(_.getOrElse("uri", "")) // group by resource
    .toList
    .sortBy(_._1.toString())
    .zipWithIndex
    .map {
      case (item, i) =>

        val row = item._2.toList
        val group: List[String] = row.map(_.getOrElse("group", "").asInstanceOf[String])
        val rank: Int = row.map(_.getOrElse("rank", "")).map(n => Integer.parseInt(n.toString())).toList(0)

        (group, rank)
    }
    .filter(_._2 == MAX_LEVELS) // start from "leaves": we want a denormalized daaset, so the upper levels are already there!
    .map(item => item._1.reverse) // order from parent to leaf (not mandatory, but useful)

    .map { items =>

      val level1 = items.map(item => std.extract_level1(vbox, item)).map(m => s"${m._1}.${m._2}.${m._3}")
      //      println("METADATA LEVEL 1: " + level1)

      val details = items.map(item => std.extract_details(vbox, item).toMap)

      // testing metadata level 1 and 2
      details.foreach { item =>

        val prps = item.toList.filter(_._1.endsWith("_uri")).distinct // extracting only property uri, by convention
        //        println(prps.mkString(" | "))

      }

      details

    } // TODO: from ?uri -> CHECK for OntologyID.ConceptID.

    .map { item =>
      println(item.size, item)
      item
    }
    .foreach { item =>
      println(item.size, item)
    }

  vbox.stop()
  catalog.stop()

}

// TODO: rename
class StdQuery(catalog: CatalogBox) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def vocabulariesWithDependencies() = catalog.vocabulariesWithDependencies()

  def vocabularyWithDependency(vocID: String) = Try {
    val res = this.vocabulariesWithDependencies()
      .filter(_.id.equals(vocID))
    if (!res.isEmpty)
      res.head
    else
      throw new RuntimeException(s"vocabulary ${vocID} not found!")
  }

  // extractions................................................................................................

  // extracting level1
  def extract_level1(vbox: VocabularyBox, uri: String) = {

    val results = SPARQL(vbox.repo).query(s"""
      SELECT DISTINCT ?ontology ?concept ?prp 
      WHERE {
        ?concept a owl:Class .
        ?concept rdfs:isDefinedBy ?ontology .
        ?uri a ?concept . ?uri ?prp [] .
      }  
    """).toList(0)

    val ontologyID = results.getOrElse("ontology", "").toString().replaceAll(".*[#/](.*)", "$1")
    val conceptID = results.getOrElse("concept", "").toString().replaceAll(".*[#/](.*)", "$1")
    val prpID = results.getOrElse("prp", "").toString().replaceAll(".*[#/](.*)", "$1")

    (ontologyID, conceptID, prpID)

  }

  def extract_hierarchy(vbox: VocabularyBox) = {
    SPARQL(vbox.repo).query("""
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      PREFIX clvapit: <http://dati.gov.it/onto/clvapit#>
      SELECT DISTINCT ?uri ?rank ?group  
      WHERE {
        ?uri a skos:Concept .
        OPTIONAL { ?uri skos:broader* ?group . }
        OPTIONAL { ?uri skos:broader ?parent . }
        OPTIONAL { { ?uri clvapit:hasRankOrder ?rank . } UNION { FILTER(!BOUND(?rank)) . BIND(1 AS ?rank) } }
      }  
    """)
  }

  def extract_levels(vbox: VocabularyBox, level_binding: String = "rank"): Int = {
    this.extract_hierarchy(vbox).map(_.getOrElse(level_binding, "0")).map(n => Integer.parseInt(n.toString())).max
  }

  def flat_hierarchy(vbox: VocabularyBox, item_binding: String = "uri") = {

    val MAX_LEVELS = this.extract_levels(vbox)

    this.extract_hierarchy(vbox)
      .groupBy(_.getOrElse(item_binding, "")) // group by resource
      .toList
      .sortBy(_._1.toString())
      .map { item => (item._1, item._2.toList) }

  }

  def extract_details(vbox: VocabularyBox, uri: String) = {

    def q_details(lang: String = "it") = {
      val filter_lang = if (lang != null) s"FILTER(?lang='${lang}')" else ""
      s"""
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        PREFIX clvapit: <http://dati.gov.it/onto/clvapit#>
        SELECT DISTINCT ?uri ?code ?label ?lang ?code_uri ?label_uri 
        WHERE {
          ?uri a skos:Concept .
          OPTIONAL { ?uri skos:notation ?code . ?uri ?code_uri ?code . }
          OPTIONAL { ?uri skos:prefLabel ?label . ?uri ?label_uri ?label . BIND(LANG(?label) AS ?lang) }
          FILTER(?uri=<${uri}>)
          ${filter_lang}
        }  
      """
    }

    SPARQL(vbox.repo).query(q_details()).headOption.getOrElse(Map.empty)
  }

}
