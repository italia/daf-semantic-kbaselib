package it.almawave.linkeddata.kb.catalog

import java.io.File
import java.net.URL
import it.almawave.linkeddata.kb.utils.JSONHelper

/**
 * refactorization of the components for standardization
 */
object TestingSKOSVocabulary extends App {

  val skos_url = new URL("https://www.w3.org/TR/skos-reference/skos.rdf")
  val skos_onto = OntologyBox.parse(skos_url)
  skos_onto.start()

  val voc_url = new File("src/test/resources/catalog-data/VocabolariControllati/licences/licences.ttl").toURI().toURL()
  val voc = VocabularyBox.parse(voc_url).federateWith(List(skos_onto))
  voc.start()

  val concepts: Stream[Map[String, Any]] = SPARQL(voc.repo).query("""
  
    PREFIX skos: <http://www.w3.org/2004/02/skos/core#> 
    SELECT ?uri ?parent_uri ?notation ?prefLabel ?altLabel 
    WHERE {
      ?uri a skos:Concept .
      ?uri skos:notation ?notation . 
      { ?uri skos:prefLabel ?prefLabel . FILTER(LANG(?prefLabel)='it') }
      OPTIONAL { ?uri skos:broader ?parent_uri . }
      OPTIONAL { ?uri skos:altLabel ?altLabel . FILTER(LANG(?altLabel)='it') }  
    }
    
    # TODO: rank
    
  """)
    .toStream

  // TODO: merge in case of multiple languages
  // TODO: compact multiple values for prop

  println("\n\n DOCS with HIERARCHIES")

  val items = HierarchyParser.expand_hierarchies(concepts, "uri", "parent_uri", "hierarchy")

  items.zipWithIndex
    .foreach {
      case (doc, i) =>

        val doc_json = JSONHelper.writeToString(doc)
        println("DOC " + i)
        println(doc_json)

    }

  val json = JSONHelper.writeToString(items)
  println(json)

  voc.stop()

}

object HierarchyParser {

  def parse_hierarchy(doc: Map[String, Any], _list: Seq[Map[String, Any]], element_field: String, parent_field: String): Stream[String] = {

    HierarchyParser
      .items_in_hierarchy(doc, _list, element_field, parent_field)
      .map(_.getOrElse(element_field, "").asInstanceOf[String])
      .reverse

  }

  def items_in_hierarchy(doc: Map[String, Any], _list: Seq[Map[String, Any]], element_field: String, parent_field: String): Stream[Map[String, Any]] = {

    // find the parent of this document
    val h_parent: Option[Map[String, Any]] = _list.find(e => e.getOrElse(element_field, "NO_ITEM").equals(doc.getOrElse(parent_field, "NO_PARENT")))
    if (h_parent.isEmpty) {
      Stream(doc)
    } else {
      Stream(doc) ++ items_in_hierarchy(h_parent.get, _list, element_field, parent_field)
    }.toStream

  }

  def expand_hierarchies(
    elements:        Seq[Map[String, Any]],
    element_field:   String,
    parent_field:    String,
    hierarchy_field: String): Seq[Map[String, Any]] = {

    elements.map { el =>

      val hierarchy = parse_hierarchy(el, elements, element_field, parent_field)
      el + (hierarchy_field -> hierarchy)

    }

  }

}
