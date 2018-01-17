package it.almawave.linkeddata.kb.repo

import it.almawave.linkeddata.kb.repo.managers._
import org.eclipse.rdf4j.repository.Repository
import org.slf4j.LoggerFactory
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import scala.util.Try
import it.almawave.linkeddata.kb.utils.TryHandlers.TryLog
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.memory.MemoryStore
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
//import it.almawave.linkeddata.kb.validator.ValidationQueryResult

/**
 *
 * IDEA: use an implicit connection
 * TODO: provide a connection pool
 * TODO: add an update method (remove + add) using the same connection/transaction
 *
 * CHECK: finally (handle connection to be closed) and/or connection pool
 * 	the idea could be encapsulating default behaviours in Try{} object as much as possible
 *  SEE (example): https://codereview.stackexchange.com/questions/79267/scala-trywith-that-closes-resources-automatically
 *
 * TODO: import codebase of Configuration wrapper
 *
 * IDEA: default usage of TryLog, wrapper for external Java API
 */
class RDFRepositoryBase(repo: Repository) {

  //  System.setProperty("org.eclipse.rdf4j.repository.debug", "false")

  //  val logger = Logger.underlying()

  implicit val logger = LoggerFactory.getLogger(this.getClass)

  // CHECK: providing custom implementation for BN

  private var conf = ConfigFactory.empty()

  def configuration(configuration: Config) = {
    conf = conf.withFallback(configuration)
  }

  def configuration(): Config = conf

  // checking if the repository is up.
  def isAlive(): Try[Boolean] = {

    TryLog {

      if (!repo.isInitialized()) repo.initialize()
      repo.getConnection.close()
      repo.shutDown()
      true

    }("repository is not reachable!")

  }

  def start() = {

    TryLog {

      if (!repo.isInitialized())
        repo.initialize()

    }(s"KB:RDF> cannot start repository!")

  }

  def stop() = {

    TryLog {

      if (repo.isInitialized())
        repo.shutDown()

    }(s"KB:RDF> cannot stop repository!")

  }

  // prefixes management
  val prefixes = new PrefixesManager(repo)

  // RDF documents management
  val store = new RDFStoreManager(repo)

  val sparql = new SPARQLManager(repo)

  val io = new RDFFileManager(this)

  val catalog = new RDFCatalogManager(this)

  // DISABLED
  
  // TODO: REFACTORING! (remove this stub)
  //  def execRuleQuery(query: String): List[ValidationQueryResult] = List.empty

  // TODO: REFACTORING! (remove this stub)
  //  def execQuery(query: String): ListBuffer[HashMap[String, String]] = new ListBuffer

}
// DISABLED
//object MainRefactoring {
//
//  val check = new RDFRepositoryBase(new SailRepository(new MemoryStore))
//
//  check.execRuleQuery("")
//
//  //  value execQuery is not a member of it.almawave.linkeddata.kb.repo.RDFRepositoryBase
//
//}
