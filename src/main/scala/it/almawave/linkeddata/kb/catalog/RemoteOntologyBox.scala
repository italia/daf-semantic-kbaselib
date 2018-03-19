package it.almawave.linkeddata.kb.catalog

import java.net.URL
import it.almawave.linkeddata.kb.parsers.OntologyParser
import it.almawave.linkeddata.kb.catalog.models.OntologyMeta
import it.almawave.linkeddata.kb.file.RDFFileRepository
import org.eclipse.rdf4j.repository.Repository

object RemoteOntologyBox {

  def parse(url: URL, rdf_source: URL) = {
    val parser = OntologyParser(rdf_source)
    val meta = parser.parse_meta().withURL(url)
    new OntologyBox(meta)
  }
}

class OLDRemoteOntologyBox(meta: OntologyMeta) extends RDFBox {

  override val id = meta.id
  override val context = meta.url.toString()

  override val repo: Repository = new RDFFileRepository(meta.source, context)

  // TODO: collecting sparql queries by configuration & convention

  override def toString() = s"""
    RemoteOntologyBox [${id}, ${status}, ${triples} triples, ${context}]
  """.trim()

  def asOntologyBox() = new OntologyBox(meta)

}

object OLDRemoteOntologyBox {

  def parse(rdf_source: URL, id: String) = {
    val parser = OntologyParser(rdf_source)
    val meta = parser.parse_meta().withID(id)
    new OLDRemoteOntologyBox(meta)
  }

  def parse(rdf_source: URL) = {
    val parser = OntologyParser(rdf_source)
    val meta = parser.parse_meta()
    new OLDRemoteOntologyBox(meta)
  }

}