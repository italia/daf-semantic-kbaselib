package it.almawave.linkeddata.kb.catalog

import java.net.URL
import java.io.File

/**
 * testing "flat" hiearchies from vocabularies, in order to use them inside the standardization process
 */
object CheckSTD2 extends App {

  val rdf_source = new File("src/test/resources/catalog-data/VocabolariControllati/licences/licences.ttl").toURI().toURL()
  val vbox = VocabularyBox.parse(rdf_source)
  vbox.start()

  val query = """
    
    PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
     
    SELECT DISTINCT *
    
    WHERE {
    
      ?uri a skos:Concept .
      ?uri skos:notation ?notation . 
      
      BIND('it' AS ?lang)
      
      OPTIONAL { ?uri skos:prefLabel ?prefLabel . FILTER(LANG(?prefLabel)=?lang) } 
      OPTIONAL { ?uri skos:altLabel ?altLabel . FILTER(LANG(?altLabel)=?lang) }
      
      OPTIONAL { ?uri skos:broader ?parent_uri . }
      
    }
    ORDER BY ?uri
    
  """

  val results = SPARQL(vbox.repo).query(query)

  results.foreach { item =>

    println(item)
    
  }

  vbox.stop()

}