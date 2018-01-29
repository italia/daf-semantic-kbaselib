package it.almawave.linkeddata.kb.parsers.meta

import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.model.BNode
import org.slf4j.LoggerFactory

class URIDependenciesExtractor(repo: Repository) {

  implicit val logger = LoggerFactory.getLogger(this.getClass)

  import it.almawave.linkeddata.kb.utils.RDF4JAdapters._

  // CHECK: RepositoryAction

  def extract() = {

    val conn = repo.getConnection

    val subs = conn.getStatements(null, null, null, true).toStream
      .map { st => st.getSubject }
      .filterNot(_.isInstanceOf[BNode])
      .map(_.stringValue())
      .map(_.replaceAll("^(.*)[#/](.*)$", "$1"))
      .distinct

    val prps = conn.getStatements(null, null, null, true).toStream
      .map { st => st.getPredicate }
      .filterNot(_.isInstanceOf[BNode])
      .map(_.stringValue())
      .map(_.replaceAll("^(.*)[#/](.*)$", "$1"))
      .distinct

    // CHECK: transitive dependencies?
    // CHECK: dependecies from explicit owl imports?

    conn.close()

    subs ++ prps

  }

}