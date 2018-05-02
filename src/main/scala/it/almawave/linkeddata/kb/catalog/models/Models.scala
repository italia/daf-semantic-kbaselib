package it.almawave.linkeddata.kb.catalog.models

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Value
import java.net.URL

import org.eclipse.rdf4j.model.Resource
import java.net.URI

import scala.collection.mutable.ListBuffer

// CHECK: ai fini della navigazione bisogna capire se usare solo titles/descriptions it

case class OntologyInformation(meta: OntologyMeta, data: RDFData)

case class VocabularyInformation(meta: VocabularyMeta, data: RDFData)

case class ItemByLanguage(lang: String, value: String)

case class URIWithLabel(value: String, uri: String, lang: String) {

  def this(value: String, uri: URI) = this(value, uri.toString(), "ita")

  def this(uri: String) = this(uri.toString().replaceAll("^.*[#/](.*?)$", "$1"), uri, "ita")

}

case class Version(
  number:  String,
  date:    String,
  comment: Map[String, String],
  uri:     String)

// TODO: add a regex / case class extractor for semantic versioning
// TODO: add a proper date format

case class LANG(code: String) // TODO

case class DateInfo(value: String)

case class OntologyMeta(

  id:        String,
  source:    URL,
  url:       URL,
  prefix:    String,
  namespace: String,
  concepts:  Set[String],
  imports:   Seq[URIWithLabel],

  titles:       Seq[ItemByLanguage],
  descriptions: Seq[ItemByLanguage],

  versions: Seq[Version],
  creators: Seq[ItemByLanguage],

  // CHECK with provenance
  publishedBy:  String,
  owner:        String,
  langs:        Seq[String], // CHECK: LANG
  lastEditDate: String,
  licenses:     Seq[URIWithLabel],

  tags:       Seq[URIWithLabel],
  categories: Seq[URIWithLabel],
  keywords:   Seq[String],
  provenance: Seq[Map[String, Any]]) {

  def withID(id_new: String) = {

    OntologyMeta(
      id_new,
      source, url,
      prefix, namespace,
      concepts, imports,
      titles, descriptions,
      versions, creators, publishedBy, owner, langs, lastEditDate, licenses,
      tags, categories, keywords, provenance)

  }

  def withURL(url_new: URL) = {

    OntologyMeta(
      id,
      source, url_new,
      prefix, namespace,
      concepts, imports,
      titles, descriptions,
      versions, creators, publishedBy, owner, langs, lastEditDate, licenses,
      tags, categories, keywords, provenance)

  }

}

// TODO: aggiornare i modelli
case class VocabularyMeta(

  id:        String,
  url:       URL,
  source:    URL,
  instances: Set[String],

  titles:       Seq[ItemByLanguage],
  descriptions: Seq[ItemByLanguage],

  publishedBy: Seq[URIWithLabel],
  owner:       Seq[URIWithLabel],
  creators:    Seq[URIWithLabel],

  langs:    Seq[String], // CHECK: LANG
  licenses: Seq[URIWithLabel],

  version: Seq[Version],

  creationDate: String,
  lastEditDate: String,
  tags:         Seq[URIWithLabel],
  categories:   Seq[URIWithLabel],
  keywords:     Seq[URIWithLabel],
  dependencies: Seq[String], // ontologies from which the vocabulary depends on
  hierarchies:  ListBuffer[Hierarchy]
)

case class RDFData(
  subjects:   Seq[Resource],
  properties: Seq[IRI],
  objects:    Seq[Value],
  contexts:   Seq[Resource])

// TODO
case class AssetType(assetType: String, representationTechnique: String)

case class Hierarchy (
  codice: String,
  label: String,
  uri: String,
  parent_uri: String,
  children: ListBuffer[Hierarchy]//offspring
)
  
