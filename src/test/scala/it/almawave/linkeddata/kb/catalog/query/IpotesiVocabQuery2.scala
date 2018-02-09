package it.almawave.linkeddata.kb.catalog.query

import it.almawave.linkeddata.kb.catalog.VocabularyBox
import java.net.URL
import it.almawave.linkeddata.kb.catalog.SPARQL
import it.almawave.linkeddata.kb.utils.JSONHelper

object IpotesiVocabQuery2 extends App {

  // # code_level_1,label_level_1,code_level_2,label_level_2,code_level_3,label_level_3
  // # A,Licenza Aperta,A.1,Dominio pubblico,A.1.1,Creative Commons CC0 1.0 Universal - Public Domain Dedication (CC0 1.0)
  // # A,Licenza Aperta,A.1,Dominio pubblico,A.1.2,ODC Public Domain Dedication and License (PDDL)

  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/Licenze/Licenze.ttl")

  val voc_box = VocabularyBox.parse(url)
  voc_box.start()

  // EX QUERY GERARCHIA (PIATTA) - ipotesi
  val items = SPARQL(voc_box.repo).query("""
    PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
    
    SELECT ?code ?label ?uri ?parent 
    
    WHERE {
    
      ?uri a skos:Concept .
      ?uri skos:notation ?code .
      ?uri skos:prefLabel ?label .
      ?uri skos:broader ?parent .
      
    }
  """)
    .map { item =>
      SKOSItem(
        item.get("uri").getOrElse(None).toString(),
        item.get("code").getOrElse(None).toString(),
        item.get("label").getOrElse(None).toString(),
        item.get("parent").getOrElse(None).toString())
    }
    .toList
    .groupBy { item => item.parent_id }
    .toList
    .sortBy{ item => item._1 }
    
    // TODO: MAKE RECURSIVE!! ;-)

  println("\n\n#### JSON")
  val json = JSONHelper.writeToString(items)
  println(json)

  case class SKOSItem(id: String, notation: String, label: String, parent_id: String)

  //
  //  results.foreach { item =>
  //    println("\n" + item._1)
  //    println(item._2.toList.map { el => (el.notation, el.label) }.mkString("\n"))
  //  }

  voc_box.stop()
}