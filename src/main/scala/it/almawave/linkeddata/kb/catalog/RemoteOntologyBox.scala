package it.almawave.linkeddata.kb.catalog

import java.net.URL
import it.almawave.linkeddata.kb.parsers.OntologyParser
import it.almawave.linkeddata.kb.catalog.models.OntologyMeta
import it.almawave.linkeddata.kb.file.RDFFileRepository
import org.eclipse.rdf4j.repository.Repository

class RemoteOntologyBox(meta: OntologyMeta) extends RDFBox {

  override val id = meta.id
  override val context = meta.url.toString()

  override val repo: Repository = new RDFFileRepository(meta.source, context)

  // TODO: collecting sparql queries by configuration & convention

  override def toString() = s"""
    RemoteOntologyBox [${id}, ${status}, ${triples} triples, ${context}]
  """.trim()
}

object RemoteOntologyBox {

  def parse(rdf_source: URL) = {
    val parser = OntologyParser(rdf_source)
    val meta = parser.parse_meta()
    new OntologyBox(meta)
  }

}