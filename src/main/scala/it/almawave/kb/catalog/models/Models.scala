package it.almawave.kb.catalog.models

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Value
import java.net.URL
import org.eclipse.rdf4j.model.Resource

// CHECK: ai fini della navigazione bisogna capire se usare solo titles/descriptions it

case class OntologyInformation(meta: OntologyMeta, data: RDFData)

case class VocabularyInformation(meta: VocabularyMeta, data: RDFData)

case class ItemByLanguage(lang:String, value:String)

case class OntologyMeta(
  id: String,
  source: URL,
  url: URL,
  prefix: String,
  namespace: String,
  concepts: Set[String],
  imports: Set[String],
  titles: Seq[(String, String)],
  descriptions: Seq[(String, String)],
  version: Seq[(String, String)],
  creators: Set[String],
  provenance: Seq[Map[String, Any]])

case class VocabularyMeta(
  id: String,
  url: URL,
  source: URL,
  concepts: Set[String],
  titles: Seq[(String, String)],
  descriptions: Seq[(String, String)],
  version: Seq[(String, String)],
  creators: Set[String])

case class RDFData(
  subjects: Set[Resource],
  properties: Set[IRI],
  objects: Set[Value],
  contexts: Set[Resource])

  
