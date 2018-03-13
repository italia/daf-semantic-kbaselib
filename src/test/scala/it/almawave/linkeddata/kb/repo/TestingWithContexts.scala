package it.almawave.linkeddata.kb.repo

import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.junit.Before
import org.junit.After
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverBase
import org.eclipse.rdf4j.query.algebra.evaluation.federation.AbstractFederatedServiceResolver
import org.eclipse.rdf4j.rio.RDFFormat
import java.net.URL
import sun.security.validator.SimpleValidator
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.rio.Rio

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import org.eclipse.rdf4j.model.Resource
import org.junit.Test
import org.eclipse.rdf4j.sail.Sail
import org.eclipse.rdf4j.sail.helpers.AbstractSail
import org.eclipse.rdf4j.sail.SailConnection
import java.nio.file.Paths

class TestingWithContexts {

  val data_dir = "src/test/resources/catalog-data"

  val onto_url = Paths.get(s"${data_dir}/Ontologie/CLV/latest/CLV-AP_IT.ttl").normalize().toUri().toString()

  val repo = new SailRepository(new MemoryStore)

  @Test
  def testing_contexts() {

    val conn = repo.getConnection

    val contexts = new ListBuffer[Resource]
    val ctxs = conn.getContextIDs
    while (ctxs.hasNext())
      contexts += ctxs.next()

    println("#### CONTEXTS")
    println(contexts.mkString("\n"))

    conn.close()
  }

  @Before
  def before() {
    repo.initialize()

    val vf = SimpleValueFactory.getInstance
    val format = Rio.getParserFormatForFileName(onto_url).get

    val conn = repo.getConnection
    conn.clear()
    conn.clear(null)
    conn.clearNamespaces()
    conn.add(new URL(onto_url), null, format, vf.createIRI("http://examples/"))
    conn.close()

  }
  @After
  def after() {
    repo.shutDown()
  }

}