package it.almawave.linkeddata.kb.catalog

import org.slf4j.LoggerFactory
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.nio.file.Paths
import it.almawave.linkeddata.kb.parsers.meta.OntologyMetadataExtractor
import it.almawave.linkeddata.kb.file.RDFFileRepository
import java.net.URL
import org.eclipse.rdf4j.repository.Repository
import scala.collection.mutable.ListBuffer
import it.almawave.linkeddata.kb.catalog.models.OntologyMeta
import it.almawave.linkeddata.kb.catalog.models.URIWithLabel
import it.almawave.linkeddata.kb.utils.URIHelper
import it.almawave.linkeddata.kb.catalog.models.ItemByLanguage
import it.almawave.linkeddata.kb.catalog.models.Version
import it.almawave.linkeddata.kb.utils.DateHelper
import it.almawave.linkeddata.kb.utils.JSONHelper
import org.eclipse.rdf4j.query.QueryLanguage
import it.almawave.linkeddata.kb.repo.RepositoryAction
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.memory.MemoryStore
import java.net.URI

object MainCatalogBox extends App {

  val conf = ConfigFactory.parseFile(Paths.get("src/main/resources/conf/catalog.conf").normalize().toFile())

  val catalog = new CatalogBox(conf)
  catalog.start()

  println("\n#### ontologies")
  catalog.ontologies.foreach(println(_))

  println("\n#### vocabularies")
  catalog.vocabularies.foreach(println(_))

  // TODO
  val ontologies_with_deps = catalog.ontologies.map { o => o.withImports() }
  // TEST: ${ontologies_with_deps}

  println(s"""
    
    #### CATALOG SUMMARY
    n° triples in catalog:       ${catalog.triples}
    
    n° ontologies loaded:        ${catalog.ontologies.size}
    n° triples in ontologies:    ${catalog.ontologies.foldLeft(0)(_ + _.triples)}
    
    n° vocabularies loaded:      ${catalog.vocabularies.size}
    n° triples in vocabularies:  ${catalog.vocabularies.foldLeft(0)(_ + _.triples)}
             with dependencies:  ${catalog.vocabulariesWithDependencies().foldLeft(0)(_ + _.triples)}
  
  """)

  catalog.stop()

  @Deprecated
  def show_concepts() {
    val counted = SPARQL(catalog.repo).query("""
    
    #SELECT (COUNT(?s) AS ?triples) 
    #WHERE { 
    #  ?s ?p ?o .
    #}
    
    SELECT DISTINCT ?graph ?concept 
    WHERE {
    
      { ?s a ?concept }
      UNION 
      { GRAPH ?graph { ?s a ?concept } }
    
    }
    
  """)
      .toStream
      .groupBy { x => x.getOrElse("graph", "") }
      .map { x => (x._1, x._2.toList.map(_.getOrElse("concept", "")).distinct) }
      .map { x => (x._1.toString().replaceAll("^.*[#/](.*?)(\\..*)*$", "$1"), x._2.map(_.toString().replaceAll("^.*[#/](.*)$", "$1"))) }

    println("COUNTED? ")
    counted.foreach { x => println(x) }
  }

}
