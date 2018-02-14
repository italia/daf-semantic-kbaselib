package it.almawave.linkeddata.kb.standardization

import it.almawave.linkeddata.kb.catalog.CatalogBox
import org.slf4j.LoggerFactory
import scala.util.Try
import it.almawave.linkeddata.kb.catalog.VocabularyBox
import it.almawave.linkeddata.kb.catalog.SPARQL
import scala.concurrent.Future
import com.google.common.util.concurrent.Futures
import scala.concurrent.impl.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class StandardizationHelper(catalog: CatalogBox) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  import scala.concurrent.ExecutionContext.Implicits.global

  // extract a list of VocabularyBox
  def vocabulariesWithDependencies(): Seq[VocabularyBox] = catalog.vocabulariesWithDependencies()

  // extract a VocabularyBox by VocabularyID
  def vocabularyWithDependency(vocID: String): Try[VocabularyBox] = Try {
    val res = this.vocabulariesWithDependencies()
      .filter(_.id.equals(vocID))
    if (!res.isEmpty)
      res.head
    else
      throw new RuntimeException(s"vocabulary ${vocID} not found!")
  }

  // extract the maximum
  def max_levels(vbox: VocabularyBox): Int = extract_hierarchy(vbox: VocabularyBox).map(_.path.size).max

  // extract hierarchy for a specific VocabularyBox
  def extract_hierarchy(vbox: VocabularyBox): Seq[Hierarchy] = {

    SPARQL(vbox.repo).query(QUERY.hierarchy())
      .groupBy(_.getOrElse("uri", "").asInstanceOf[String]).toList
      .sortBy(_._1)
      .map { el =>
        Hierarchy(el._1, el._2.map(_.getOrElse("group", "").asInstanceOf[String]).toList.distinct)
      }

  }

  // this method creates a de-normalized, standardized version for the vocabulary/dataset
  def standardize_data(vbox: VocabularyBox, lang: String = "it") = {

    // we should know the maximum levels in hierarchies, in order to fill them, if needed
    val MAX_LEVELS = this.max_levels(vbox)

    // 1) the vocabulary is decomposed in a list of hierarchies, each one derived from each leaf
    val hierarchies = extract_hierarchy(vbox)

    // 2) each hierarchy is then expandend with details for each element in the path
    hierarchies.toStream.map { hierarchy =>

      type ROW = List[(String, Any)] // TODO: parse to case class!
      // NOTE: the internal metadata (level 1, level2, type... could be saved properly)
      // NOTE: List[(String, Any)] <-> Map[String, Any]

      //      case class DTS_CELL()

      val testing: List[Future[ROW]] = hierarchy.path
        .zipWithIndex
        .map {

          case (uri, l) =>

            val level = l + 1

            // we use a Future here to try improving performances when collecting results from many SPARQL queries
            Future {
              SPARQL(vbox.repo).query(QUERY.details(vbox.id, level, uri, lang))
                .toList.flatMap(_.toList).toList
                .map(el => (el._1 + "_level" + level, el._2))
            }

          // CHECK: lookup of property into the existing ontologies! -> OntologyID.ConceptID.propertyID
          // CHECK: FILL: List.fill(MAX_LEVELS - _details.size)(null)

        }

      // List[Future] -> Future[List]
      val futs_seq = Future.sequence(testing)

      // awaiting all the computed futures
      Await.result(futs_seq, Duration.Inf)
    }

  }

  // MODELS
  case class Hierarchy(uri: String, path: List[String])

  object QUERY {

    /**
     * We can extract the path (a local hierarchy)
     * to each instance (leaf) of a target concept in the vocabulary.
     * The idea is to construct a list of paths composed of URIs,
     * which will be expanded with their details in a second phase.
     */
    def hierarchy() = {
      """
        # QUERY FOR SKOS!
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        PREFIX clvapit: <http://dati.gov.it/onto/clvapit#>
        SELECT DISTINCT ?uri ?group 
        WHERE {
          ?uri a skos:Concept .
          ?uri skos:broader* ?group .
        }
        ORDER BY ?uri ?group 
      """
    }

    /**
     * This method internally uses a query to expand details about an individual.
     * We need to use some convention, in order to handle the metadata for internal usage:
     *  + _type_{field}
     *  	is used for describing the datatype for each cell/column, inferred from SPARQL/RDF.
     *  	TODO: conversion of the RDF4J datatypes to a selection of meaningful Java corrispondences
     * 	+ _meta1_{field}
     * 		is used for the metadata level 1, eg: `SKOS.Concept.notation`
     * 	+ _meta2 is actually _meta2_{field}
     * 		is used for the metadata level 2, eg: `Licenze.level1`
     */
    def details(vocabularyID: String, level: Int, uri: String, lang: String) = {

      s"""
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        PREFIX clvapit: <http://dati.gov.it/onto/clvapit#>
        
        SELECT DISTINCT ?code ?_type_code ?_meta1_code ?label ?_type_label ?_meta1_label ?_meta2
        
        WHERE {
          
          ?uri a skos:Concept .
          
          OPTIONAL { 
            ?uri skos:notation ?code . 
            BIND("SKOS.Concept.notation" AS ?_meta1_code) .    # metadata 1 level
            BIND(DATATYPE(?code) AS ?_type_code)
          }
          OPTIONAL { 
            ?uri skos:prefLabel ?label . 
            BIND("SKOS.Concept.prefLabel" AS ?_meta1_label) .  # metadata 1 level
            BIND(LANG(?label) AS ?label_lang) .
            BIND(DATATYPE(?label) AS ?_type_label) .
          }
          
          BIND("${vocabularyID}.level_${level}" AS ?_meta2)
          
          FILTER(?uri=<${uri}>)
          FILTER(?label_lang="${lang}")
          
        } 
        ## concept code label
        """
    }

  }

}