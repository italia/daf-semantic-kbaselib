package it.almawave.linkeddata.kb.catalog

import java.net.URL

object TestingOntoWithSKOS extends App {

  val skos_url = new URL("https://www.w3.org/TR/skos-reference/skos.rdf")

  val onto = OntologyBox.parse(skos_url)
  onto.start()

  val concepts = SPARQL(onto.repo).query("""
  
    SELECT ?concept ?prp  
    WHERE {
      ?concept a owl:Class .
      ?concept ?prp [] . 
    }  
    
  """).toList

  println("SKOS concepts:")
  concepts.foreach { concept =>

    println(concept)

  }

  onto.stop()

}