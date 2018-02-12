package it.almawave.linkeddata.kb.standardization

import java.nio.file.Paths
import com.typesafe.config.ConfigFactory
import org.eclipse.rdf4j.sail.federation.Federation
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.memory.MemoryStore
import scala.collection.mutable.ListBuffer
import java.net.URL
import it.almawave.linkeddata.kb.catalog.CatalogBox
import it.almawave.linkeddata.kb.catalog.OntologyBox
import it.almawave.linkeddata.kb.catalog.SPARQL

object MainStandardization extends App {

  val conf = ConfigFactory.parseFile(Paths.get("src/main/resources/conf/catalog.conf").normalize().toFile())
  val catalog = new CatalogBox(conf)
  catalog.start()

  val std = new Standardization(catalog)
  std.vocabularies_standardization()

  catalog.stop()

}

class Standardization(catalogBox: CatalogBox) {

  val conf = catalogBox.conf

  def vocabularies_standardization() {

    catalogBox.vocabularies.foreach { box =>

      println("\n\n\ndependencies.............................................")

      println("\n\n#### VOCABULARY: " + box)
      val onto_baseURI = conf.getString("ontologies.baseURI")
      println("onto_baseURI : " + onto_baseURI)

      val _deps = box.meta.dependencies
        .filter { d => d.startsWith(onto_baseURI) }

      println("network deps for " + box.context + " : " + _deps.mkString("|"))

      val _ontos = _deps.foldLeft(new ListBuffer[OntologyBox]) { (loaded, d) =>
        val dep = new URL(d)
        val onto_ctx = OntologyBox.parse(dep).context
        val onto_loaded = catalogBox.ontologies.find { item =>
          item.context.trim().equals(onto_ctx.trim())
        }
        if (onto_loaded.isDefined)
          loaded += onto_loaded.get
        else
          loaded
      }.toList

      println(s"__________________________ ${box} depends on ONTOLOGIES:")
      _ontos.foreach { o =>
        println(o)
      }
      println(s"__________________________")

      // OK!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
      // TODO: parametrization

      val federation = new Federation
      federation.addMember(new SailRepository(new MemoryStore)) // avoiding federation of nothing!
      federation.addMember(box.repo) // vocabulary itself
      _ontos.foreach(o => federation.addMember(o.repo))
      _ontos.foreach(o => o.repo.shutDown())
      val exp = new SailRepository(federation)
      exp.initialize()
      println("")

      println("ASK..................................................")
      // CHECK FOR the ontology type....
      // network ontology, using rank order

      def use_l0() = {
        SPARQL(exp).ask(s"""
            PREFIX clvapit: <http://dati.gov.it/onto/clvapit#>
            ASK { 
              ?concept a owl:Class . 
              ?uri a ?concept . 
              ?uri clvapit:hasRankOrder [] 
            }
          """)
      }

      def use_skos() = {
        SPARQL(exp).ask(s"""
            PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
            PREFIX clvapit: <http://dati.gov.it/onto/clvapit#>
            ASK { 
              ?vocab a skos:ConceptScheme .  
            }
          """)
      }

      if (use_l0) {

        SPARQL(exp).query(s"""
            PREFIX clvapit: <http://dati.gov.it/onto/clvapit#>
            PREFIX dc: <http://purl.org/dc/elements/1.1/>
            SELECT DISTINCT ?ontology_uri ?concept ?prp ?vocabulary_uri ?rank ?code ?label 
            WHERE { 
              ?uri a ?concept .
              ?concept a owl:Class .
              OPTIONAL { ?concept rdfs:label ?concept_label . }
              OPTIONAL { ?concept dc:title ?concept_label . }  
              ?concept rdfs:isDefinedBy ?ontology_uri .
              #?uri ?prp [] .
              OPTIONAL { ?uri clvapit:hasIdentifier ?code . }
              OPTIONAL { ?uri clvapit:name ?label . }
              OPTIONAL { ?uri clvapit:hasRankOrder ?rank . } .
              BIND("${box.context}" AS ?vocabulary_uri) .
            }
          """)
          .map { item =>

            val ontoID = item.getOrElse("ontology_uri", "").toString().replaceAll(".*[/#](.*)", "$1")
            val conceptID = item.getOrElse("concept", "").toString().replaceAll(".*[/#](.*)", "$1")
            val prp = item.getOrElse("prp", "").toString().replaceAll(".*[/#](.*)", "$1")
            val rank = item.getOrElse("rank", "").toString().replaceAll(".*[/#](.*)", "$1")
            val code = item.getOrElse("code", "").toString().replaceAll(".*[/#](.*)", "$1")
            val label = item.getOrElse("label", "").toString().replaceAll(".*[/#](.*)", "$1")

            println(ontoID, conceptID)

            item
          }
          .foreach { item => println(item) }

        exp.shutDown()

      } else if (use_skos) {

        println("SKOS.hierarchy ????")
        SPARQL(exp).query(s"""
            PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
            PREFIX clvapit: <http://dati.gov.it/onto/clvapit#>
            PREFIX dc: <http://purl.org/dc/elements/1.1/>
            SELECT DISTINCT ?ontology_uri ?concept ?prp ?vocabulary_uri ?rank ?code ?label 
            WHERE { 
              ?uri a ?concept . ?uri a skos:Concept .
              OPTIONAL { ?concept rdfs:isDefinedBy ?ontology_uri . }
              ?uri ?prp [] . 
              OPTIONAL { ?uri skos:isDefinedBy ?vocabulary_uri . }
              OPTIONAL { ?uri skos:notation ?code . }
              OPTIONAL { ?uri skos:prefLabel ?label . FILTER(LANG(?label)='it') }
              OPTIONAL { ?uri clvapit:hasRankOrder ?rank . } .
            }
          """)
          .map { item =>
            val ontoID = item.getOrElse("ontology_uri", "").toString().replaceAll(".*[/#](.*)", "$1")
            val conceptID = item.getOrElse("concept", "").toString().replaceAll(".*[/#](.*)", "$1")
            val prp = item.getOrElse("prp", "").toString().replaceAll(".*[/#](.*)", "$1")
            val vocabID = item.getOrElse("vocabulary_uri", "").toString().replaceAll(".*[/#](.*)", "$1")
            val rank = item.getOrElse("rank", "").toString()
            val code = item.getOrElse("code", "").toString().trim()
            val label = item.getOrElse("label", "").toString().trim()

            Map(
              "ontologyID" -> "SKOS",
              "conceptID" -> conceptID,
              "vocabID" -> vocabID, "level" -> rank, "code" -> code, "label" -> label)

            (s"SKOS.${conceptID}.${prp}")
          }
          .distinct
          .foreach { item => println(item) }

      } else {
        System.err.println(s"cannot find a suitable SPARQL query for vocabulary ${box}")
      }

      println("dependencies.............................................\n\n\n")

    }

  }

}