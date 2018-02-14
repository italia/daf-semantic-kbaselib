package it.almawave.linkeddata.kb.standardization

import it.almawave.linkeddata.kb.catalog.CatalogBox
import org.slf4j.LoggerFactory
import scala.util.Try
import it.almawave.linkeddata.kb.catalog.VocabularyBox
import it.almawave.linkeddata.kb.catalog.SPARQL

class StandardizationHelper(catalog: CatalogBox) {

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

  def max_levels(vbox: VocabularyBox) = extract_hierarchy(vbox: VocabularyBox).map(_.path.size).max

  def extract_hierarchy(vbox: VocabularyBox) = {

    // TODO: prefixes
    // TODO: concept
    // TODO: parent_relation

    SPARQL(vbox.repo).query("""
      # QUERY FOR SKOS!
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      PREFIX clvapit: <http://dati.gov.it/onto/clvapit#>
      SELECT DISTINCT ?uri ?group 
      WHERE {
        ?uri a skos:Concept .
        ?uri skos:broader* ?group .
      }
      ORDER BY ?uri ?group 
    """)
      //      .map{ item => val x = item.getOrElse("group", "").asInstanceOf[String]; println(x) ; item }
      .groupBy(_.getOrElse("uri", "").asInstanceOf[String]).toList
      .sortBy(_._1)
      .map { el =>
        Hierarchy(el._1, el._2.map(_.getOrElse("group", "").asInstanceOf[String]).toList.distinct)
      }

  }

  def standardize_data(vbox: VocabularyBox) = {

    val hierarchies = this.extract_hierarchy(vbox)

    val MAX_LEVELS = this.max_levels(vbox)

    // we should create a final output conforming to those levels
    println("MAX_LEVELS: " + MAX_LEVELS)

    val results = hierarchies.map { item =>

      println("\n## .........................................................")
      println(item)

      item.path
        .zipWithIndex
        .map {

          case (uri, l) =>
            // TODO std.extract_details(vbox, instance)

            val lang = "it"
            val level = l + 1

            // TODO: externalize query...
            def _details(uri: String, lang: String) = {
              SPARQL(vbox.repo).query(s"""
                PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
                PREFIX clvapit: <http://dati.gov.it/onto/clvapit#>
                
                SELECT DISTINCT ?code ?_meta_1_code ?label ?_meta_1_label ?_meta_2
                
                WHERE {
                  
                  ?uri a skos:Concept .
                  
                  OPTIONAL { 
                    ?uri skos:notation ?code . 
                    BIND("SKOS.Concept.notation" AS ?_meta_1_code)
                  }
                  OPTIONAL { 
                    ?uri skos:prefLabel ?label . 
                    BIND("SKOS.Concept.prefLabel" AS ?_meta_1_label)
                    BIND(LANG(?label) AS ?label_lang) 
                  }
                  
                  BIND("${vbox.id}.level_${level}" AS ?_meta_2)
                  
                  FILTER(?uri=<${uri}>)
                  FILTER(?label_lang="${lang}")
                  
                } 
                ## concept code label
              """)
                .toList.flatMap(_.toList).toList
                .map(el => (el._1 + "_level" + level, el._2))
            }

            // TODO: lookup of property into the existing ontologies! -> OntologyID.ConceptID.propertyID

            // CHECK: FILL: List.fill(MAX_LEVELS - _details.size)(null)

            _details(uri, lang)
          //              .toMap // TODO: verify order!

        }

    }

    results
  }

  // MODELS
  case class Hierarchy(uri: String, path: List[String])

}