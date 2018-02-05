package it.almawave.linkeddata.kb.repo

import org.eclipse.rdf4j.repository.RepositoryConnection
import org.slf4j.Logger
import scala.util.Failure
import scala.util.Try
import org.eclipse.rdf4j.repository.Repository
import scala.util.Success
import scala.concurrent.Future

/*
   * this could be useful for simplifying code:
   * 	+ default connection handling (open/close)
   * 	+ default transaction handling
   *
   * RepositoryAction(repo){ conn =>
   * 	...
   * }("some error message...")
   *
   */
object RepositoryAction {

  def apply[R](repo: Repository)(conn_action: (RepositoryConnection => Any))(msg_err: String)(implicit logger: Logger): Try[R] = {

    // CHECK: verify the repository is active! otherwise, activate it
    //    var repo_was_active = repo.isInitialized()
    //    if (!repo_was_active) repo.initialize()

    // NOTE: we could imagine using a connection pool here
    val _conn = repo.getConnection

    _conn.begin()

    val results: Try[R] = try {

      val success = Success(conn_action(_conn))
      _conn.commit()
      success.asInstanceOf[Try[R]]

    } catch {

      case ex: Throwable =>
        val failure = Failure(ex)
        _conn.rollback()
        logger.info(msg_err)
        failure

    }

    _conn.close()

    // return to the original repository active / not active state
    //    if (!repo_was_active) repo.shutDown()

    // gets the result as a Future
    //  TODO:  Future.fromTry(results)

    results
  }

}