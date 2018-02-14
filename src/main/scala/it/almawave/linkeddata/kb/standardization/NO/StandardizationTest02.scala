package it.almawave.linkeddata.kb.standardization.NO

import com.typesafe.config.ConfigFactory
import it.almawave.linkeddata.kb.catalog.CatalogBox
import java.nio.file.Paths
import org.slf4j.LoggerFactory
import scala.util.Try
import it.almawave.linkeddata.kb.catalog.VocabularyBox
import it.almawave.linkeddata.kb.catalog.SPARQL

object StandardizationTest02 extends App {

  val conf = ConfigFactory.parseFile(Paths.get("src/main/resources/conf/catalog.conf").normalize().toFile())
  val catalog = new CatalogBox(conf)
  catalog.start() // TODO: bind start/stop/status

  val std = new CatalogStandardizationWrapper(catalog)

  val vocID = "AccommodationTypology"
  val vbox: VocabularyBox = std.vocabularyWithDependency(vocID).get
  vbox.start()

  val voc_std = new VocabularyStandardizationWrapper(vbox)

  val hierarchy = voc_std.extract_hierarchy().toList
  hierarchy.foreach(println(_))

  vbox.stop()
  catalog.stop()

}

class VocabularyStandardizationWrapper(vbox: VocabularyBox) {

  def getQBox() = {

    // TODO: get them from vocabulary / configuration
    val prefixes = List(
      Prefix("skos", "http://www.w3.org/2004/02/skos/core#"),
      Prefix("clvapit", "http://dati.gov.it/onto/clvapit#"))

    // TODO: get them from configuration
    val q_box = QBox(List("skos:Concept"), prefixes, "skos:broader")

    q_box
  }

  def extract_hierarchy() = {

    def q_hierarchy(qbox: QBox) = {

      val q_prefixes = qbox.prefixes.map(p => s"PREFIX ${p.prefix}: <${p.namespace}> ").mkString("\n")
      val q_filter_concepts = qbox.concepts.map(c => s"""FILTER (?concept = ${c})""").mkString("\n")

      s"""
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      PREFIX clvapit: <http://dati.gov.it/onto/clvapit#>
      SELECT DISTINCT ?uri ?rank ?group  
      WHERE {
        ?uri a skos:Concept .
        OPTIONAL { ?uri skos:broader* ?group . }
        OPTIONAL { ?uri skos:broader ?parent . }
        OPTIONAL { { ?uri clvapit:hasRankOrder ?rank . } UNION { FILTER(!BOUND(?rank)) . BIND(1 AS ?rank) } }
      }   
      """

    }

    val qh = q_hierarchy(this.getQBox())

    //http://dati.gov.it/onto/controlledvocabulary/AccoTypology/A4,
    //List(
    //    Map(
    //        rank -> 2,
    //        uri -> http://dati.gov.it/onto/controlledvocabulary/AccoTypology/A4,
    //        group -> http://dati.gov.it/onto/controlledvocabulary/AccoTypology/A4),
    //    Map(rank -> 3,
    //        uri -> http://dati.gov.it/onto/controlledvocabulary/AccoTypology/A41,
    //        group -> http://dati.gov.it/onto/controlledvocabulary/AccoTypology/A4),
    //   Map(rank -> 4,
    //       uri -> http://dati.gov.it/onto/controlledvocabulary/AccoTypology/A411,
    //       group -> http://dati.gov.it/onto/controlledvocabulary/AccoTypology/A4)))

    println("SPARQL> executing " + qh)
    SPARQL(vbox.repo).query(qh)
      .groupBy(_.getOrElse("group", "").asInstanceOf[String])
      .map(el => el._2.toList.map( el => el.getOrElse("uri", "").asInstanceOf[String] ))

    //      .map(el => (el._1, el._2.map(_.getOrElse("group", "").asInstanceOf[String]).toList))

  }

}

case class Prefix(prefix: String, namespace: String)

case class QBox(
  concepts:           Seq[String],
  prefixes:           Seq[Prefix],
  relation_hierarchy: String)

class CatalogStandardizationWrapper(catalog: CatalogBox) {

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

}


