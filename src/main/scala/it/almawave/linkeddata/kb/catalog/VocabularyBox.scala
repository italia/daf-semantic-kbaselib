package it.almawave.linkeddata.kb.catalog

import it.almawave.linkeddata.kb.file.RDFFileRepository
import org.eclipse.rdf4j.repository.Repository
import java.net.URL
import org.slf4j.LoggerFactory
import it.almawave.linkeddata.kb.catalog.models.VocabularyMeta
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.memory.MemoryStore
import it.almawave.linkeddata.kb.catalog.models.URIWithLabel
import it.almawave.linkeddata.kb.catalog.models.ItemByLanguage
import it.almawave.linkeddata.kb.utils.URIHelper
import it.almawave.linkeddata.kb.catalog.models.Version
import it.almawave.linkeddata.kb.catalog.models.AssetType
import it.almawave.linkeddata.kb.parsers.VocabularyParser

object VocabularyBox {
  def parse(rdf_source: URL) = {
    val parser = VocabularyParser(rdf_source)
    val meta = parser.parse_meta()
    new VocabularyBox(meta)
  }
}

class VocabularyBox(val meta: VocabularyMeta) extends RDFBox {

  override val id = meta.id
  override val context = meta.url.toString()

  override val repo: Repository = new RDFFileRepository(meta.source, context)

  // TODO: collecting sparql queries by configuration & convention

  override def toString() = s"""
    VocabularyBox [${id}, ${status}, ${triples} triples, ${context}]
  """.trim()

}
