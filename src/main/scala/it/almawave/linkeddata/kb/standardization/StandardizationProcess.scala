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
import it.almawave.linkeddata.kb.utils.JSONHelper
import scala.util.Success
import scala.util.Failure

/**
 * This class adds support for the extraction of metadata,
 * needed to run the standardization process for a dataset on a vocabulary.
 */
class StandardizationProcess(catalog: CatalogBox) {

  import scala.concurrent.ExecutionContext.Implicits.global
  private val logger = LoggerFactory.getLogger(this.getClass)

  // extracts a list of VocabularyBox
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

  // extracts the deeper level of hierarchy
  def max_levels(vbox: VocabularyBox): Try[Int] = {
    extract_hierarchy(vbox: VocabularyBox) match {
      case Success(h)   => Success(h.map(_.path.size).max)
      case Failure(err) => Failure(err) // CHECK: maybe 0?
    }
  }

  // extract hierarchy for a specific VocabularyBox
  def extract_hierarchy(vbox: VocabularyBox): Try[Seq[Hierarchy]] = Try {

    // TODO: JUNit test for hierarchy!! CHECK: orders of items in path

    // extract the full path / hierarchy to an individual (the uri represents the individual we start from)
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
    val hierarchies = extract_hierarchy(vbox).getOrElse(List())

    // 2) each hierarchy is then expandend with details for each element in the path
    hierarchies.toStream.map { hierarchy =>

      type GROUP_CELLS = List[(String, Any)] // TODO: parse to case class!
      // NOTE: the internal metadata (level 1, level2, type... could be saved properly)
      // NOTE: List[(String, Any)] <-> Map[String, Any]

      val future_with_cells: Seq[Future[Seq[Cell]]] = hierarchy.path
        .zipWithIndex
        .map {

          case (uri, l) =>

            val level = l + 1

            // we use a Future here to try improving performances when collecting results from many SPARQL queries
            val fut_group: Future[List[Cell]] = Future {

              /*
               *  TODO: we should avoid do several times the same query!
               *  IDEA:
               *  	1) collect all the URIs for resources
               *  	2) launch queries
               *  	3) using results as an in-memory cache
               *
               *  IDEA: Future{  SPARQL(vbox.repo).query(QUERY.details(vbox.id, level, uri, lang)) } ...
               */
              val group_cells: GROUP_CELLS = SPARQL(vbox.repo)
                .query(QUERY.details(vbox.id, level, uri, lang))
                .toList.flatMap(_.toList).toList
                .map(el => (s"${el._1}_level${level}", el._2))

              // actual fields (no internal metadata)
              val fields = group_cells.toList.map(_._1).toList
                .filter(_.contains("_level"))
                .filterNot(_.contains("_meta"))
                .filterNot(_.contains("_type"))

              // results as Map
              val map = group_cells.toMap

              /*
               *  each record has a number of pseudo-columns, used to add internal metadata informations:
               *  we can parse them into a proper structured Cell object, in order to have all the data
               *  available for further flexible processing
               */
              fields.map { field_name =>

                val field_value = map.getOrElse(field_name, "").asInstanceOf[Object]
                val field_datatype = map.getOrElse(s"_type_${field_name}", "String").asInstanceOf[Object]
                val field_meta1 = map.getOrElse(s"_meta1_${field_name}", "UnknownOntology.UnknownConcept.unkownProperty").asInstanceOf[String]
                val field_meta2 = group_cells.filter(_._1.contains("_meta2")).headOption.map(_._2).getOrElse(s"UnknownVocabulary.level${level}").asInstanceOf[String]

                Cell(
                  uri,
                  field_name,
                  field_value,
                  field_datatype,
                  field_meta1,
                  field_meta2)
              }

            }

            fut_group

          // CHECK: lookup of property into the existing ontologies! -> OntologyID.ConceptID.propertyID
          // CHECK: FILL: List.fill(MAX_LEVELS - _details.size)(null)

        }
        .toStream

      // List[Future] -> Future[List]
      // la future contiene una lista di gruppi di celle, da ri-organizzare prima dell'output!
      val futs_seq: Future[Seq[Cell]] = Future.sequence(future_with_cells)
        .map { el => el.flatMap(_.toList).sortBy { cell => cell.uri } }

      // awaiting all the computed futures
      Await.result(futs_seq, Duration.Inf)
    }

  }

  // MODELS ....................................................................
  case class Hierarchy(uri: String, path: List[String])

  // TODO: move elesewhere
  case class Cell(
    uri:         String,
    name:        String,
    value:       Object,
    datatype:    Object,
    meta_level1: String,
    meta_level2: String)

  object Cell {
    val EMPTY = Cell(
      "uri://unknown",
      "", "",
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString",
      "UnknowOntology.UnknownConcept.unkownProperty", "UnknownVocabulary.level1")
  }
  // MODELS ....................................................................

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