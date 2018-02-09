package it.almawave.linkeddata.kb.parsers

import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.slf4j.LoggerFactory
import it.almawave.linkeddata.kb.file.RDFFileRepository
import java.net.URL
import org.eclipse.rdf4j.repository.Repository
import it.almawave.linkeddata.kb.catalog.SPARQL
import it.almawave.linkeddata.kb.catalog.models.VocabularyMeta
import it.almawave.linkeddata.kb.catalog.models.URIWithLabel
import it.almawave.linkeddata.kb.catalog.models.ItemByLanguage
import it.almawave.linkeddata.kb.utils.URIHelper
import it.almawave.linkeddata.kb.catalog.models.Version
import it.almawave.linkeddata.kb.catalog.models.AssetType
import java.net.URI
import org.eclipse.rdf4j.common.iteration.Iterations

object VocabularyParser {

  def apply(rdf_source: URL) = {
    val repo: Repository = new RDFFileRepository(rdf_source)
    new VocabularyParser(repo, rdf_source)
  }

}

// TODO: move into parser package
class VocabularyParser(repo: Repository, rdf_source: URL) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  // the repository should be active! ASK: how can we avoid this check?
  { if (!repo.isInitialized()) repo.initialize() }

  logger.debug(s"parsing metadata for ${rdf_source}")

  //  val repo: Repository = new RDFFileRepository(rdf_source)
  val sparql = SPARQL(repo)

  val id: String = rdf_source.getPath.replaceAll(".*/(.*)\\.[a-z]+$", "$1").trim()

  def parse_meta(): VocabularyMeta = {

    val voc_url = this.parse_voc_url()

    val instances = this.parse_instances()
    val titles = this.parse_titles()
    val descriptions = this.parse_descriptions()

    val creators = this.parse_creators()
    val owner = this.parse_owner()
    val publishedBy = this.parse_publishedBy()
    val langs = this.parse_langs()
    val licenses = this.parse_licenses()
    val versions = this.parse_versions()
    val lastEditDate = this.parse_lastEditDate()

    val tags: Seq[URIWithLabel] = this.parse_dc_subjects()
    val categories: Seq[URIWithLabel] = this.parse_dcat_themes()
    val keywords: Seq[URIWithLabel] = this.parse_dcat_keywords()

    val dependencies: Seq[String] = this.parse_dependencies()

    VocabularyMeta(
      id,
      voc_url,
      rdf_source,
      instances,
      titles,
      descriptions,
      publishedBy,
      owner,
      creators,
      langs,
      licenses,
      versions,
      lastEditDate,
      tags,
      categories,
      keywords,
      dependencies)
  }

  def parse_voc_url(): URL = {
    sparql.query("""
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT ?uri 
        WHERE { ?uri a skos:ConceptScheme . }
      """)
      .map { item => item.getOrElse("uri", "").asInstanceOf[String] }
      .map { item => new URL(item) }
      .headOption.getOrElse(rdf_source)
  }

  def parse_instances(): Set[String] = {
    sparql.query("""
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT ?concept 
        WHERE { 
          ?concept a ?klass. 
          ?klass rdfs:subClassOf* skos:Concept . 
        } 
        """)
      .map(_.getOrElse("concept", "owl:Thing").toString()).toSet
  }

  def parse_titles(): Seq[ItemByLanguage] = {
    sparql.query(s"""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT * 
        WHERE { 
          ?uri a skos:ConceptScheme . 
          OPTIONAL { ?uri rdfs:label ?label . BIND(LANG(?label) AS ?lang) }
          OPTIONAL { ?uri dct:title ?label . BIND(LANG(?label) AS ?lang) } 
        }
        """)
      .map { item => ItemByLanguage(item.getOrElse("lang", "").asInstanceOf[String], item.getOrElse("label", "").asInstanceOf[String]) }
      .toList
  }

  def parse_descriptions(): Seq[ItemByLanguage] = {
    sparql.query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT ?lang ?label  
        WHERE { 
          ?uri a skos:ConceptScheme . 
          OPTIONAL { ?uri rdfs:comment ?label . BIND(LANG(?label) AS ?lang) }
          OPTIONAL { ?uri dct:description ?label . BIND(LANG(?label) AS ?lang) } 
        }
        """)
      .map { item => ItemByLanguage(item.getOrElse("lang", "").asInstanceOf[String], item.getOrElse("label", "").asInstanceOf[String]) }
      .toList
  }

  def parse_publishedBy(): String = {
    sparql.query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT * 
        WHERE { ?uri a skos:ConceptScheme . ?uri dct:publisher ?publisher_uri .}  
      """)
      .map { item => item.getOrElse("publisher_uri", "").asInstanceOf[String] }
      .headOption.getOrElse("")
  }

  def parse_creators(): Seq[Map[String, String]] = {
    List() // TODO
  }

  def parse_owner(): String = {
    sparql.query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT * 
        WHERE { ?uri a skos:ConceptScheme . ?uri dct:rightsHolder ?holder_uri .}  
      """)
      .map { item => item.getOrElse("holder_uri", "").asInstanceOf[String] }
      .headOption.getOrElse("")
  }

  def parse_langs(): Seq[String] = {
    sparql.query("""
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT ?lang  
        WHERE {
          ?uri a skos:ConceptScheme . 
          ?uri ?prp ?label . BIND(LANG(?label) AS ?lang) .
        }  
      """)
      .map { item => item.getOrElse("lang", "").asInstanceOf[String] }
      .filterNot { item => item.trim().equalsIgnoreCase("") }
  }

  def parse_licenses(): Seq[URIWithLabel] = {
    sparql.query("""
        PREFIX dcat: <http://www.w3.org/ns/dcat#>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        PREFIX dct: <http://purl.org/dc/terms/>
        SELECT DISTINCT ?license_uri 
        WHERE {
          ?uri a skos:ConceptScheme . 
          ?uri dcat:distribution ?distribution . ?distribution dct:license ?license_uri .
        }
      """)
      .map(_.getOrElse("license_uri", "").asInstanceOf[String])
      .filterNot(_.equalsIgnoreCase(""))
      .map { item =>
        val uri = item
        val label = URIHelper.extractLabelFromURI(uri)
        val lang = ""
        URIWithLabel(label, uri, lang)
      }
  }

  def parse_versions(): Seq[Version] = {
    sparql.query("""
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT * 
        WHERE { 
          ?uri a skos:ConceptScheme . 
          ?uri owl:versionInfo ?version_info  
        }
      """)
      .map { item => item.getOrElse("version_info", "").asInstanceOf[String] }
      .map { item =>
        val number = item
        Version(number, null, null, null)
      }
  }

  def parse_lastEditDate() = {
    sparql.query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT ?date_modified 
        WHERE { ?uri a skos:ConceptScheme . ?uri dct:modified ?date_modified . }  
      """)
      .map { item => item.getOrElse("date_modified", "").asInstanceOf[String] }
      .headOption.getOrElse("")
  }

  def parse_asset_type() = {
    AssetType("taxonomy", "SKOS") // TODO: extract from vocabulary!!
  }

  def parse_dcat_keywords() = {
    sparql.query("""
      PREFIX dcat: <http://www.w3.org/ns/dcat#>
      PREFIX dct: <http://purl.org/dc/terms/>
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      SELECT DISTINCT ?keyword ?lang 
      WHERE { ?uri a skos:ConceptScheme . ?uri dcat:keyword ?keyword . BIND(LANG(?keyword) AS ?lang) } 
    """)
      .map { item =>
        val label = item.getOrElse("keyword", "").asInstanceOf[String].trim()
        val lang = item.getOrElse("lang", "").asInstanceOf[String]
        val uri = s"keywords://${lang}#${label.replaceAll("\\s", "+")}"
        URIWithLabel(label, uri, lang)
      }
  }

  def parse_dcat_themes() = {
    sparql.query("""
      PREFIX dcat: <http://www.w3.org/ns/dcat#>
      PREFIX dct: <http://purl.org/dc/terms/>
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      SELECT DISTINCT ?theme 
      WHERE { 
        ?uri a skos:ConceptScheme . ?uri dcat:theme ?theme .
      } 
    """)
      .map { item => item.getOrElse("theme", "").asInstanceOf[String] }
      .map { item =>
        val uri = item
        val label = URIHelper.extractLabelFromURI(uri)
        val lang = "" // NOTE: can we assume dcat themes is defined for "eng" language?
        URIWithLabel(label, uri, lang)
      }
  }

  def parse_dc_subjects() = {
    sparql.query("""
      PREFIX dct: <http://purl.org/dc/terms/>
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      SELECT DISTINCT ?subject  
      WHERE { ?uri a skos:ConceptScheme . ?uri dct:subject ?subject . } 
    """)
      .map { item => item.getOrElse("subject", "").asInstanceOf[String] }
      .map { item =>
        val uri = item
        val label = URIHelper.extractLabelFromURI(uri)
        val lang = ""
        URIWithLabel(label, uri, lang)
      }
  }

  def parse_dependencies(): Seq[String] = {

    import scala.collection.JavaConversions._
    import scala.collection.JavaConverters._

    SPARQL(repo).query("""
      SELECT DISTINCT ?concept 
      WHERE {
        ?subject a ?concept 
      }  
    """)
      .map(item => item.toMap.getOrElse("concept", "").asInstanceOf[String])
      .map(item => item.replaceAll("^(.*)[#/].*?$", "$1"))
      .distinct

  }

  // should we shutdown the repository after using it?
  if (!repo.isInitialized()) repo.initialize()
}

object TestingBaseExtraction extends App {

  val url = "http://dati.gov.it/onto/clvapit#Feature"

  val txt = url.replaceAll("^(.*)[#/].*?$", "$1")
  println("verify: " + txt)

}

// CHECK
trait Parser {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  protected val repo = new SailRepository(new MemoryStore)

  // the repository should be active! ASK: how can we avoid this check?
  { if (!repo.isInitialized()) repo.initialize() }

  // should we shutdown the repository after using it?
  if (!repo.isInitialized()) repo.initialize()

}

