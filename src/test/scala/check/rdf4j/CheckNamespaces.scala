package check.rdf4j

import java.net.URL
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.eclipse.rdf4j.repository.Repository
import it.almawave.linkeddata.kb.file.RDFFileRepository

// check namespaces directly frm data
import it.almawave.linkeddata.kb.utils.RDF4JAdapters._
import org.eclipse.rdf4j.model.BNode
import scala.collection.mutable.ListBuffer

object CheckNamespaces extends App {

  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/Organizzazioni/latest/COV-AP_IT.ttl")

  val repo: Repository = new RDFFileRepository(url)
  repo.initialize()

  // CHECK: find a good default when namespaces are not explicitly provided
  val conn = repo.getConnection
  val namespaces = conn.getNamespaces.toList
  println("\nNAMESPACES: ")
  namespaces.foreach { x => println(x) }

  println("\nCONTEXTIDS: ")
  conn.getContextIDs.foreach { cx => println(cx) }

  // TEST for guessing a set of used namespaces
  val subs = conn.getStatements(null, null, null, true).toStream
    .map { st => st.getSubject }
    .filterNot(_.isInstanceOf[BNode])
    .map(_.stringValue())
    .map(_.replaceAll("^(.*)[#/](.*)$", "$1")).distinct

  val prps = conn.getStatements(null, null, null, true).toStream
    .map { st => st.getPredicate }
    .filterNot(_.isInstanceOf[BNode])
    .map(_.stringValue())
    .map(_.replaceAll("^(.*)[#/](.*)$", "$1")).distinct

  println("\nDEPENDENCIES: ")
  val dependencies: Stream[String] = subs ++ prps
  dependencies.foreach { x => println(x) }

  conn.close()

  repo.shutDown()

}