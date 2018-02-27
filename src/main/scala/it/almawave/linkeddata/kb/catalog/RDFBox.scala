package it.almawave.linkeddata.kb.catalog

import org.slf4j.LoggerFactory

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.common.iteration.Iterations
import org.eclipse.rdf4j.model.Resource

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.IRI
import it.almawave.linkeddata.kb.catalog.models.RDFData
import java.security.MessageDigest
import scala.util.Random
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.sail.Sail
import java.net.URL
import it.almawave.linkeddata.kb.file.RDFFileRepository

object RDFBox {

  def parse(rdf_source: URL): RDFBox = {

    new RDFBox {

      override val repo = new RDFFileRepository(rdf_source)

    }

  }

}

/**
 * an RDFBox is a component that acts as a simple container of RDF informations:
 * internally it relies to some specific RDF Repository implementation, but wraps its logic adding more tailored functions.
 */
trait RDFBox {

  import it.almawave.linkeddata.kb.utils.RDF4JAdapters._

  protected val conf: Config = ConfigFactory.empty()
  protected val logger = LoggerFactory.getLogger(this.getClass)

  protected val repo: Repository = new SailRepository(new MemoryStore)

  // creating a random ID
  val id = java.util.UUID.randomUUID().toString
  val context = s"repo://${id}"

  def start() {
    if (!repo.isInitialized()) repo.initialize()
  }

  def stop() {
    if (repo.isInitialized()) repo.shutDown()
  }

  def status = if (repo.isInitialized()) "active" else "inactive"

  // REVIEW / MOVE
  def concepts = {

    import it.almawave.linkeddata.kb.utils.RDF4JAdapters._

    if (!repo.isInitialized()) repo.initialize()
    val conn = repo.getConnection
    val concepts = conn.prepareTupleQuery(QueryLanguage.SPARQL, s"""
      SELECT DISTINCT ?concept ?property 
      FROM <${context}>
      WHERE { 
          ?s a ?concept . 
          OPTIONAL { ?s ?property [] }
          OPTIONAL { ?concept a owl:Class . ?property rdfs:domain ?concept }
      }
    """)
      .evaluate().toList
      .map { x =>
        val concept = x.getBinding("concept").getValue.stringValue()
        val property = x.getBinding("property").getValue.stringValue()
          .replaceAll("^.*[#/](.*?)$", "$1")
        (curie(concept), curie(property))
      }
      .sorted
      .groupBy(_._1)
      .map { item => (item._1, item._2.toList.map(_._2)) }

    conn.close()

    concepts
  }

  private def curie(uri: String, prefix: Boolean = false) = {
    uri.replaceAll("^.*[#/](.*?)$", "$1")
  }

  def triples = {
    val conn = repo.getConnection
    val triples = conn.prepareTupleQuery(QueryLanguage.SPARQL, """
      SELECT ?graph ?sub ?prp ?obj
      WHERE { 
        { ?sub ?prp ?obj }
        UNION
        { GRAPH ?graph { ?sub ?prp ?obj } }
      }
    """).evaluate().toList.size
    conn.close()
    triples
  }

  def statements = {
    if (!repo.isInitialized()) repo.initialize()
    val conn = repo.getConnection
    val ctx = SimpleValueFactory.getInstance.createIRI(context)
    val statements = conn.getStatements(null, null, null, true)
    conn.close()
    statements
  }

  // useful for testing
  def parseData() = {
    val conn = repo.getConnection
    val contextsIDS: Seq[Resource] = Iterations.asList(conn.getContextIDs)
    val subjects: Seq[Resource] = Iterations.asList(conn.getStatements(null, null, null, true)).toStream.map { st => st.getSubject }.distinct.toSeq
    val properties: Seq[IRI] = Iterations.asList(conn.getStatements(null, null, null, true)).toStream.map { st => st.getPredicate }.distinct.toSeq
    val objects: Seq[Value] = Iterations.asList(conn.getStatements(null, null, null, true)).toStream.map { st => st.getObject }.distinct.toSeq
    val contexts: Seq[Resource] = Iterations.asList(conn.getStatements(null, null, null, true)).toStream.map { st => st.getContext }.distinct.toSeq
    conn.close()
    RDFData(subjects, properties, objects, contexts ++ contextsIDS)
  }

}

