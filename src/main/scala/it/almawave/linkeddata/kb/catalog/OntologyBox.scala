package it.almawave.linkeddata.kb.catalog

import java.net.URL
import org.eclipse.rdf4j.repository.Repository
import org.slf4j.LoggerFactory

import it.almawave.linkeddata.kb.utils.URIHelper
import it.almawave.linkeddata.kb.utils.DateHelper
import it.almawave.linkeddata.kb.file.RDFFileRepository
import it.almawave.linkeddata.kb.catalog.models.Version
import it.almawave.linkeddata.kb.catalog.models.ItemByLanguage
import it.almawave.linkeddata.kb.catalog.models.OntologyMeta
import it.almawave.linkeddata.kb.catalog.models.URIWithLabel
import it.almawave.linkeddata.kb.parsers.OntologyParser
import org.eclipse.rdf4j.query.QueryLanguage

object OntologyBox {

  /**
   * creates an OntologyBox object,
   * after parsing and collecting metadata about the ontology
   */
  def parse(rdf_source: URL) = {
    val parser = OntologyParser(rdf_source)
    val meta = parser.parse_meta()
    new OntologyBox(meta)
  }

}

/**
 * CHECK: we should review the policy to check weather a repository is initialized or not
 *
 * TODO: refactorize with meta import
 */
class OntologyBox(val meta: OntologyMeta) extends RDFBox {

  override val id = meta.id
  override val context = meta.url.toString()

  override val repo: Repository = new RDFFileRepository(meta.source, context)

  // TODO: collecting sparql queries by configuration & convention

  override def toString() = s"""
    OntologyBox [${id}, ${status}, ${triples} triples, ${context}]
  """.trim()

  def withImports(): OntologyBox = {
    val rdf_imports = meta.imports.map(_.uri).map(new URL(_))
    new OntologyBoxWithImports(meta)
  }

}

// creating the internal RDFFIleRepository from all the rdf sources
class OntologyBoxWithImports(meta: OntologyMeta) extends OntologyBox(meta) {

  val _imports = meta.imports.map { x => new URL(x.uri) }.toList
  override val repo: Repository = new RDFFileRepository(meta.source :: _imports, meta.url.toString())

}
