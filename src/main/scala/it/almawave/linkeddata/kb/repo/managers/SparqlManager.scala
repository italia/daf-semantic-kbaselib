package it.almawave.linkeddata.kb.repo.managers

import scala.util.Try
import scala.concurrent.Future
import scala.collection.mutable.ListBuffer

import it.almawave.linkeddata.kb.utils.TryHandlers._
import it.almawave.linkeddata.kb.utils.RDF4JAdapters._
import it.almawave.linkeddata.kb.repo.RepositoryAction

import java.util.{ HashMap => JHashMap, Map => JMap, Collections => JCollections }

import org.eclipse.rdf4j.query.TupleQuery
import org.slf4j.LoggerFactory
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.query.QueryLanguage

/*
 * this part can be seen as a sparql datastore abstraction
 * 
 * TODO: merge the methods from the initial version
 * 
 */
class SPARQLManager(repo: Repository) {

  implicit val logger = LoggerFactory.getLogger(this.getClass)

  def query(_query: String)(implicit inferred: Boolean = false): Try[Seq[Map[String, Object]]] = {

    RepositoryAction(repo) { conn =>

      // REVIEW: performances
      // TODO: rewrite to a more general method, including GraphQuery
      val tuple_query: TupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, _query)

      tuple_query.setIncludeInferred(inferred)

      tuple_query.evaluate()
        .toStream
        .map(_.toMap())
        .toList

    }(s"SPARQL> cannot execute query\n${_query}")

  }

}
