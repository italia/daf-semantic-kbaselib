package examples.rdf4j.fts

import java.util.Properties
import org.eclipse.rdf4j.sail.solr.SolrIndex
import org.eclipse.rdf4j.sail.lucene.LuceneSail
import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.eclipse.rdf4j.repository.sail.SailRepository
import java.net.URL
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import org.eclipse.rdf4j.model.Statement
import it.almawave.linkeddata.kb.file.RDFFileSail

//import examples.rdf4j.RDFFileSail

/*
 * 	This example aims to be a starting point for an implementation using indexing with triples, for example with Solr
 *  NOTE: the package fts provides also Elasticsearch support, even if not updated to the latest version
 *
 *  TODO: review the example
 */
object ExampleSolrRDFFile extends App {

  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/COV/latest/COV-AP_IT.ttl")

  val source_sail = new RDFFileSail(List(url))

  val source_repo = new SailRepository(source_sail)
  if (!source_repo.isInitialized())
    source_repo.initialize()

  val rconn = source_repo.getConnection

  val statements = new ListBuffer[Statement]
  val sts = rconn.getStatements(null, null, null, true)
  while (sts.hasNext())
    statements += sts.next()

  statements.foreach { st => println(st) }

  if (source_repo.isInitialized())
    source_repo.shutDown()

}

class SolrRDFFile {

  // ---------------------------------------------------------------------------

  val source_sail = new MemoryStore

  val index = new SolrIndex()
  val sailProperties = new Properties()
  sailProperties.put(SolrIndex.SERVER_KEY, "embedded:")
  index.initialize(sailProperties)
  val client = index.getClient()

  org.eclipse.rdf4j.common.concurrent.locks.Properties.setLockTrackingEnabled(true)
  val lucenesail = new LuceneSail()
  lucenesail.setBaseSail(source_sail)
  lucenesail.setLuceneIndex(index)
  lucenesail.setParameter(LuceneSail.INDEX_CLASS_KEY, classOf[SolrIndex].getName())
  lucenesail.setParameter(SolrIndex.SERVER_KEY, "embedded:")
  val repo = new SailRepository(lucenesail)

  // ---------------------------------------------------------------------------

}
