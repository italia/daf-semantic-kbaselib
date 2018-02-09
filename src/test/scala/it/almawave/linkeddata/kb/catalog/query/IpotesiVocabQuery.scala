package it.almawave.linkeddata.kb.catalog.query

import it.almawave.linkeddata.kb.catalog.VocabularyBox
import java.net.URL
import it.almawave.linkeddata.kb.catalog.SPARQL

object IpotesiVocabQuery extends App {

  // # code_level_1,label_level_1,code_level_2,label_level_2,code_level_3,label_level_3
  // # A,Licenza Aperta,A.1,Dominio pubblico,A.1.1,Creative Commons CC0 1.0 Universal - Public Domain Dedication (CC0 1.0)
  // # A,Licenza Aperta,A.1,Dominio pubblico,A.1.2,ODC Public Domain Dedication and License (PDDL)

  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/Licenze/Licenze.ttl")

  val voc_box = VocabularyBox.parse(url)
  voc_box.start()

  //  SPARQL(voc_box.repo).query("""
  //  """)
  //  .foreach { item => println(item.map(el => s"${el._1}:${el._2}").mkString(" | ")) }

  // EX QUERY GERARCHIA (PIATTA) - ipotesi
  val items = SPARQL(voc_box.repo).query("""
    
    # DEFAULT query for extracting details and parents in skos
    
    PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
    SELECT DISTINCT 
    ?uri ?id ?label ?notation ?scheme ?property_name ?property_value 
    ?parent_id ?parent_uri 
    
    WHERE {
    
      ?uri a skos:Concept .
      
      ?uri skos:inScheme ?_scheme .
      ?uri skos:notation ?notation .
      ?uri skos:prefLabel ?label .
      OPTIONAL { ?uri skos:broader ?parent_uri . BIND(REPLACE(STR(?parent_uri), '^.*[/#](.*)$', '$1') AS ?parent_id) }
      
      BIND(REPLACE(STR(?uri), '^.*[/#](.*)$', '$1') AS ?id)
      BIND(REPLACE(STR(?_scheme), '^.*[/#](.*)$', '$1') AS ?scheme)
      
      {
        ?uri ?p ?property_value . 
        BIND(REPLACE(STR(?p), '^.*[/#](.*)$', '$1') AS ?property_name) 
        FILTER(?p NOT IN (rdf:type, skos:inScheme, skos:notation, skos:prefLabel))
      }
      
    }
  """)

  case class SKOSItem(id: String, notation: String, label: String, parent_id: String)

  val results = items
    .map { item =>
      SKOSItem(
        item.get("id").getOrElse(None).toString(),
        item.get("notation").getOrElse(None).toString(),
        item.get("label").getOrElse(None).toString(),
        item.get("parent_id").getOrElse(None).toString())
    }
    .toList
    .groupBy { item => item.parent_id }

  results.foreach { item =>
    println("\n" + item._1)
    println(item._2.toList.map { el => (el.notation, el.label) }.mkString("\n"))
  }

  //    .filter { item => item.forall { key => !key._1.equalsIgnoreCase("property_name") } }
  //    .filter { item => item.forall { key => !key._1.equalsIgnoreCase("property_value") } }
  //  items.foreach { item => println(item.map(el => s"${el._1}:${el._2}").mkString(" | ")) }

  voc_box.stop()
}