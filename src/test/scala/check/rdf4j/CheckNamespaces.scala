package check.rdf4j

import java.net.URL
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.eclipse.rdf4j.repository.Repository
import it.almawave.linkeddata.kb.file.RDFFileRepository

object CheckNamespaces extends App {

  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/Organizzazioni/latest/COV-AP_IT.ttl")

  val repo: Repository = new RDFFileRepository(url)

  val conn = repo.getConnection

  conn.getNamespaces.hasNext()
  
  conn.close()

  repo.shutDown()

}