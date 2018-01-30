package it.almawave.linkeddata.kb.catalog.models

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Value
import java.net.URL
import org.eclipse.rdf4j.model.Resource
import java.net.URI
import java.util.Date

// CHECK: ai fini della navigazione bisogna capire se usare solo titles/descriptions it

case class OntologyInformation(meta: OntologyMeta, data: RDFData)

case class VocabularyInformation(meta: VocabularyMeta_NEW, data: RDFData)

case class ItemByLanguage(lang: String, value: String)

case class URIWithLabel(label: String, uri: String) {

  def this(label: String, uri: URI) = this(label, uri.toString())

  def this(uri: String) = this(uri.toString().replaceAll("^.*[#/](.*?)$", "$1"), uri)

}

case class Version(
  number: String,
  date: String,
  comment: Map[String, String],
  uri: String)

// TODO: add a regex / case class extractor for semantic versioning
// TODO: add a proper date format

case class LANG(code: String) // TODO

case class DateInfo(value: String)

case class OntologyMeta(

  id: String,
  source: URL,
  url: URL,
  prefix: String,
  namespace: String,
  concepts: Set[String],
  imports: Seq[URIWithLabel],
  titles: Map[String, String], // CHECK: Seq[ItemByLanguage] ADD ItemsByLanaguages as Map[ItemByLanaguage]
  descriptions: Map[String, String],
  versions: Seq[Version],
  creators: Seq[Map[String, String]],

  // CHECK with provenance
  publishedBy: String,
  owner: String,
  langs: Seq[String], // CHECK: LANG
  lastEditDate: String,
  licenses: Seq[URIWithLabel],

  tags: Seq[URIWithLabel],
  categories: Seq[URIWithLabel],
  keywords: Seq[String],
  // CHECK with provenance

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

// TODO: aggiornare i modelli
case class VocabularyMeta_NEW(
  id: String,
  url: URL,
  source: URL,
  instances: Set[String],
  titles: Map[String, String],
  descriptions: Map[String, String],

  publishedBy: String, // TODO
  owner: String, // TODO
  creators: Seq[Map[String, String]], // TODO

  langs: Seq[String], // CHECK: LANG
  licenses: Seq[URIWithLabel],

  version: Seq[(String, String)],

  lastEditDate: String,
  tags: Seq[URIWithLabel],
  categories: Seq[URIWithLabel],
  keywords: Seq[String])

case class RDFData(
  subjects: Set[Resource],
  properties: Set[IRI],
  objects: Set[Value],
  contexts: Set[Resource])

case class AssetType(assetType: String, representationTechnique: String)
  
  
/*
 * CHECK for case class -> Map conversion:

def getCCParams(cc: AnyRef) = (Map[String, Any]() /: cc.getClass.getDeclaredFields) 
	{
	(a, f) =>
  	f.setAccessible(true)
  	a + (f.getName -> f.get(cc))
	}

 */

