package it.almawave.linkeddata.kb.repo.managers

import scala.util.Try
import scala.concurrent.Future

import org.slf4j.LoggerFactory

import org.eclipse.rdf4j.model.vocabulary._
import org.eclipse.rdf4j.repository.Repository

//import it.almawave.linkeddata.kb.utils.TryHandlers._
//import it.almawave.linkeddata.kb.utils.RDF4JAdapters._
//import it.almawave.linkeddata.kb.repo.RepositoryAction
import it.almawave.linkeddata.kb.repo.RepositoryAction
import it.almawave.linkeddata.kb.utils.RDF4JAdapters._

/**
 * This is a facility class for handling the prefixes explicitly.
 * This may be useful when construting queries, in particular.
 */
class PrefixesManager(repo: Repository) {

  implicit val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * clearing namespaces
   */
  def clear() = {

    RepositoryAction(repo) { conn =>

      conn.clearNamespaces()

    }(s"KB:RDF> error while removing namespaces!")

  }

  /**
   * adding prefix/namespace pairs
   */
  def add(namespaces: (String, String)*) {

    RepositoryAction(repo) { conn =>

      namespaces.foreach { pair => conn.setNamespace(pair._1, pair._2) }
      conn.commit()

    }(s"KB:RDF> cannot add namespaces: ${namespaces}")

  }

  /**
   * removing prefix/namespace pairs
   */
  def remove(namespaces: (String, String)*) {

    RepositoryAction(repo) { conn =>

      namespaces.foreach { pair => conn.setNamespace(pair._1, pair._2) }
      conn.commit()

    }(s"KB:RDF> cannot remove namespaces: ${namespaces}")

  }

  /**
   * gets a map of prefixes
   */
  def list(): Try[Map[String, String]] = {

    RepositoryAction(repo) { conn =>

      conn.getNamespaces.toList
        .map { ns => (ns.getPrefix, ns.getName) }
        .toMap

    }("cannot retrieve a list of prefixes")

  }

  val DEFAULT = Map(
    OWL.PREFIX -> OWL.NAMESPACE,
    RDF.PREFIX -> RDF.NAMESPACE,
    RDFS.PREFIX -> RDFS.NAMESPACE,
    DC.PREFIX -> DC.NAMESPACE,
    FOAF.PREFIX -> FOAF.NAMESPACE,
    SKOS.PREFIX -> SKOS.NAMESPACE,
    XMLSchema.PREFIX -> XMLSchema.NAMESPACE,
    FN.PREFIX -> FN.NAMESPACE,
    "doap" -> DOAP.NAME.toString(), // SEE: pull request
    "geo" -> GEO.NAMESPACE,
    SD.PREFIX -> SD.NAMESPACE)

}