package examples.rdf4j

import java.io.File
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager
import org.eclipse.rdf4j.repository.config.RepositoryConfig
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig
import org.eclipse.rdf4j.sail.inferencer.fc.config.ForwardChainingRDFSInferencerConfig
import org.eclipse.rdf4j.rio.Rio
import java.io.StringReader
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.net.URL

/**
 * IDEA: this should be used as a basic example for refactorization of existing repositories in the catalog
 */
object ExampleRepositoryManager extends App {

  // CHECK  val remote_manager = new RemoteRepositoryManager("http://localhost:8080/rdf4j-server")

  val baseDir = new File("target/RDF4J/data/")
  val manager = new LocalRepositoryManager(baseDir)

  if (!manager.isInitialized())
    manager.initialize()

  // this could be from file!
  val repo_spec_01 = new SailRepositoryConfig(new MemoryStoreConfig())
  val repo_config_01 = new RepositoryConfig("REPO_01", repo_spec_01)
  manager.addRepositoryConfig(repo_config_01)

  // 2nd!
  val repo_spec_02 = new SailRepositoryConfig(new ForwardChainingRDFSInferencerConfig(new MemoryStoreConfig(true)))
  val repo_config_02 = new RepositoryConfig("REPO_02", repo_spec_01)
  manager.addRepositoryConfig(repo_config_02)

  val rdf_config = """  

    @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
    @prefix rep: <http://www.openrdf.org/config/repository#>.
    @prefix sr: <http://www.openrdf.org/config/repository/sail#>.
    @prefix sail: <http://www.openrdf.org/config/sail#>.
    @prefix ms: <http://www.openrdf.org/config/sail/memory#>.
    @prefix ns: <http://www.openrdf.org/config/sail/native#>.
    
    [] a rep:Repository ;
       rep:repositoryID "memory" ;
       rdfs:label "Memory store" ;
       rep:repositoryImpl [
          rep:repositoryType "openrdf:SailRepository" ;
          sr:sailImpl [
             sail:sailType "openrdf:MemoryStore" ;
             ms:persist true ;
             ms:syncDelay 0 ;
             ns:tripleIndexes "spoc,posc" ;
          ]
       ]
    .
    
  """

  val spec_model = Rio.parse(new StringReader(rdf_config), "", RDFFormat.TURTLE)
  val vf = SimpleValueFactory.getInstance
  val repo_config_03 = RepositoryConfig.create(spec_model, vf.createIRI("http://examples/repo_03/"))
  repo_config_03.setID("REPO_03") // ERR: "Literal label cannot be null" ???
  repo_config_03.setTitle("in.memory")

  manager.addRepositoryConfig(repo_config_03)

  // ---- USAGE ----------------------------------------------------------------------------

  val infos = manager.getAllRepositoryInfos(true)

  infos.toList.foreach { info =>

    println(s"\nREPO: ${info.getId}")
    println(info.getLocation)
    println(info.getDescription)

  }

  val repo_01 = manager.getRepository("REPO_01")
  val conn_01 = repo_01.getConnection

  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/ClassificazioniPerServiziPubblici/CanaliErogazione/Channel.ttl")
  conn_01.add(
    url.openStream(),
    "", RDFFormat.TURTLE)

  conn_01.getStatements(null, null, null).asList().toList
    .foreach { st => println(st) }
  conn_01.close()

  if (manager.isInitialized())
    manager.shutDown()

}