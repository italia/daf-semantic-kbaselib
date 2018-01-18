package examples.rdf4j

import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.sail.memory.MemoryStore
import java.net.URL
import org.eclipse.rdf4j.repository.sail.SailRepository
import scala.collection.mutable.ListBuffer
import org.eclipse.rdf4j.model.Statement
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

class RDFFileSail(url: URL) extends MemoryStore {

  System.setProperty("org.eclipse.rdf4j.repository.debug", "true")

  //  CHECK: RDFFileSail(url: URL, sail: Sail)

  override def initialize() {

    super.initialize()

    val input = url.openStream()
    val base_url = url.toString()

    val sconn = super.getConnection
    sconn.begin()
    Rio.parse(input, base_url, Rio.getParserFormatForFileName(url.getPath).get)
      .toList
      .foreach { st =>
        sconn.addStatement(st.getSubject, st.getPredicate, st.getObject, st.getContext)
      }
    sconn.commit()
    sconn.close()

    input.close()
  }

  override def shutDown() {
    super.shutDown()
  }

}

object MainRDFFileSail extends App {

  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/Organizzazioni/latest/COV-AP_IT.ttl")

  val source_sail = new RDFFileSail(url)

  val source_repo = new SailRepository(source_sail)
  if (!source_repo.isInitialized())
    source_repo.initialize()

  val rconn = source_repo.getConnection

  val statements = new ListBuffer[Statement]
  val sts = rconn.getStatements(null, null, null, true)
  while (sts.hasNext())
    statements += sts.next()

  statements.foreach { st => println(st) }

  rconn.close()

  if (source_repo.isInitialized())
    source_repo.shutDown()

} 