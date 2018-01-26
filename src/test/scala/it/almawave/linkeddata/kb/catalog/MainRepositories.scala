package it.almawave.linkeddata.kb.catalog

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.File
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.memory.MemoryStore

object MainRepositories extends App {

  val conf = ConfigFactory.parseFile(new File("src/main/resources/conf/catalog.conf"))

  new RDFCatalog(conf)

}

/*
 * we need a basic class for 
 * - handling all the RDF sources
 * - creating a new repository for each RDF (evaluating if we need to import dependencies too)
 * - creating a general repository
 * - adding an internal SPARQL endpoint (eventually mapped to an HTTP endpoint)
 * 
 * rdfcatalog/
 * 	ontologies/
 * 		ontology/{ontology_id}
 * 	vocabularies/
 * 		vocabulary/{vocabulary_id}
 * 
 */
class RDFCatalog(configuration: Config) {

  val repo: Repository = new SailRepository(new MemoryStore())

  // at startup we should load default configs, then override (jgit?)
  val _conf = configuration

}

