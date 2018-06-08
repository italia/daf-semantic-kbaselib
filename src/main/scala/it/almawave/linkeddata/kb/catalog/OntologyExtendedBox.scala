package it.almawave.linkeddata.kb.catalog

import java.net.URL

import it.almawave.linkeddata.kb.catalog.models.OntologyExtendedMeta
import it.almawave.linkeddata.kb.file.RDFFileRepository
import it.almawave.linkeddata.kb.parsers.OntologyExtendedParser
import it.almawave.linkeddata.kb.utils.URLHelper
import org.eclipse.rdf4j.repository.Repository

object OntologyExtendedBox {

  /**
   * creates an OntologyBox object,
   * after parsing and collecting metadata about the ontology
   */
  def parse(rdf_source1: URL, rdf_source2: URL) = {
    val parser = OntologyExtendedParser(rdf_source1, rdf_source2)
    val meta = parser.parse_meta(rdf_source2.toString)
    new OntologyExtendedBox(meta)
  }

}

/**
 * CHECK: we should review the policy to check weather a repository is initialized or not
 *
 * TODO: refactorize with meta import
 */
class OntologyExtendedBox(val meta: OntologyExtendedMeta) extends RDFBox {

  override val id = meta.id
  override val context = meta.url.toString()

  override val repo: Repository = new RDFFileRepository(meta.source, context)

  // TODO: collecting sparql queries by configuration & convention

  override def toString() = s"""
    OntologyBox [${id}, ${status}, ${triples} triples, ${context}]
  """.trim()

  def withImports(): OntologyExtendedBox = {
    val rdf_imports = meta.imports.map(_.uri).map(u => URLHelper.follow_redirect(new URL(u)))
    new OntologyExtendedBoxWithImports(meta)
  }

}

// creating the internal RDFFIleRepository from all the rdf sources
class OntologyExtendedBoxWithImports(meta: OntologyExtendedMeta) extends OntologyExtendedBox(meta) {

  val _imports = meta.imports.map { x => new URL(x.uri) }.toList
  override val repo: Repository = new RDFFileRepository(meta.source :: _imports, meta.url.toString())

}



