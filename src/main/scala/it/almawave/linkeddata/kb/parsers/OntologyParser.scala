package it.almawave.linkeddata.kb.parsers

import java.net.URL
import org.eclipse.rdf4j.repository.Repository
import it.almawave.linkeddata.kb.file.RDFFileRepository
import org.slf4j.LoggerFactory
import it.almawave.linkeddata.kb.catalog.SPARQL
import it.almawave.linkeddata.kb.catalog.models.OntologyMeta
import it.almawave.linkeddata.kb.catalog.models.URIWithLabel
import it.almawave.linkeddata.kb.utils.URIHelper
import it.almawave.linkeddata.kb.catalog.models.ItemByLanguage
import it.almawave.linkeddata.kb.catalog.models.Version
import it.almawave.linkeddata.kb.utils.DateHelper

object OntologyParser {

  def apply(rdf_source: URL) = {
    val repo: Repository = new RDFFileRepository(rdf_source)
    new OntologyParser(repo, rdf_source)
  }

}

// TODO: move into parser package
class OntologyParser(val repo: Repository, rdf_source: URL) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  // the repository should be active! ASK: how can we avoid this check?
  { if (!repo.isInitialized()) repo.initialize() }

  logger.debug(s"parsing metadata for ${rdf_source}")

  //  val repo: Repository = new RDFFileRepository(rdf_source)
  val sparql = SPARQL(repo)

  val id: String = rdf_source.getPath.replaceAll(".*/(.*)\\.[a-z]+$", "$1").trim()
  val prefix: String = id.replaceAll("_", "").replaceAll("-", "").toLowerCase().trim()

  def parse_meta(): OntologyMeta = {

    val namespace = this.parse_namespace()
    val onto_url = this.parse_onto_url()
    val concepts = this.parse_concepts()
    val imports = this.parse_imports()
    val titles = this.parse_titles()
    val descriptions = this.parse_descriptions()
    val creators = this.parse_creators()
    val versions = this.parse_versions()
    val langs = this.parse_langs()

    val publishedBy: String = ""
    val owner: String = ""
    val licenses = this.parse_licenses()

    val lastEditDate: String = this.parse_lastEditDate()

    val tags: Seq[URIWithLabel] = List()
    val categories: Seq[URIWithLabel] = List()
    val keywords: Seq[String] = List()

    val provenance = this.parse_provenance()

    OntologyMeta(
      id,
      rdf_source,
      onto_url,
      prefix,
      namespace,
      concepts,
      imports,
      titles,
      descriptions,
      versions,
      creators,
      publishedBy,
      owner,
      langs,
      lastEditDate,
      licenses,
      tags,
      categories,
      keywords,
      provenance)

  }

  def parse_namespace(default_namespace: String = ""): String = {
    sparql.query("""
      SELECT DISTINCT ?uri { ?uri a owl:Ontology . }
    """)
      .map(_.getOrElse("uri", default_namespace).asInstanceOf[String])
      .map { _.replaceAll("^(.*)[/#]$", "$1") } // hack for avoid using both `http://example/` and `http://example`
      .distinct
      .toSet
      .headOption.getOrElse("")
      .toString()
  }

  def parse_onto_url(): URL = {
    val _onto_url = sparql.query("""
        SELECT DISTINCT ?uri 
        WHERE { ?onto_uri rdf:type owl:Ontology ; rdfs:isDefinedBy ?uri . }
      """)
    val onto_url = if (_onto_url.isEmpty)
      rdf_source
    else
      new URL(_onto_url(0)("uri").asInstanceOf[String].replaceAll("(.*)[#/]", "$1"))

    onto_url
  }

  def parse_concepts(): Set[String] = {
    sparql.query("""
        SELECT DISTINCT ?concept 
        WHERE { ?concept a owl:Class . FILTER(!isBlank(?concept)) } 
      """)
      .map(_.getOrElse("concept", "owl:Thing").toString()).toSet
  }

  def parse_imports(): Seq[URIWithLabel] = {
    sparql.query("""
        SELECT DISTINCT ?import_uri 
        WHERE {  ?uri a owl:Ontology . ?uri owl:imports ?import_uri . }
      """)
      .map(_.get("import_uri").get.asInstanceOf[String])
      .map { item =>
        val uri = item.trim()
        val label = URIHelper.extractLabelFromURI(item)
        val lang = ""
        URIWithLabel(label, uri, lang)
      }
  }

  def parse_titles(): Seq[ItemByLanguage] = {
    sparql.query("""
        SELECT DISTINCT * 
        WHERE {  ?uri a owl:Ontology . ?uri rdfs:label ?label . BIND(LANG(?label) AS ?lang) }
      """)
      .map { item =>
        ItemByLanguage(item.get("lang").get.asInstanceOf[String], item.get("label").get.asInstanceOf[String])
      }
      .toList
  }

  def parse_descriptions(): Seq[ItemByLanguage] = {
    // TODO: case class | Map
    sparql.query("""
        SELECT DISTINCT * 
        WHERE {  ?uri a owl:Ontology . ?uri rdfs:comment ?comment . BIND(LANG(?comment) AS ?lang) }
      """)
      .map { item => ItemByLanguage(item.get("lang").get.asInstanceOf[String], item.get("comment").get.asInstanceOf[String]) }
      .toList
  }

  def parse_creators(): Seq[Map[String, String]] = {
    sparql.query("""
        PREFIX dc: <http://purl.org/dc/elements/1.1/>
        SELECT DISTINCT ?lang ?creator 
        WHERE { 
          ?uri a owl:Ontology . ?uri dc:creator ?creator . BIND(LANG(?creator) AS ?lang) .
        }
      """)
      .toList
      .map { item =>
        val _creator = item.getOrElse("creator", "").asInstanceOf[String]
        Map("label" -> _creator, "lang" -> item.getOrElse("lang", "").asInstanceOf[String])
      }
  }

  def parse_versions(): Seq[Version] = {
    // TODO: case class
    sparql.query("""
        SELECT DISTINCT * 
        WHERE {  
          ?uri a owl:Ontology . 
          ?uri owl:versionIRI ?version_iri .
      		?uri owl:versionInfo ?version_info . BIND(LANG(?version_info) AS ?lang) .
    		}""")
      .map { item =>

        // TODO: simplify!

        val lang = item.getOrElse("lang", "").asInstanceOf[String]

        val _matcher = "^.*?(\\d+\\.\\d+(\\.\\d+)*).*\\s+-\\s+(.*)\\s+-\\s+(.*)$"
        val _vv = item.getOrElse("version_info", "").asInstanceOf[String]
        val number = _vv.replaceAll(_matcher, "$1")
        val source_date = _vv.replaceAll(_matcher, "$3")

        val _comment = _vv.replaceAll(_matcher, "$4")
        val uri = item.getOrElse("uri", "").asInstanceOf[String]
        val version_iri = item.getOrElse("version_iri", "").asInstanceOf[String]

        val comment = Map(lang -> _comment)
        val date = DateHelper.format(DateHelper.parseDate(source_date))

        Version(number, date, comment, uri)
      }

    // TODO: .groupBy { x => x.uri } for collapsing multiple versions (by lang) into one
  }

  def parse_provenance(): Seq[Map[String, Any]] = {
    sparql.query("""
        PREFIX dc: <http://purl.org/dc/elements/1.1/>
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX prov: <http://www.w3.org/ns/prov#>
        SELECT DISTINCT * {  
          ?uri a owl:Ontology . 
      		OPTIONAL { ?uri dct:license ?license_uri . }
          OPTIONAL { ?uri dct:issued ?date_issued . }
          OPTIONAL { ?uri dct:modified ?date_modified . }
          OPTIONAL { ?uri rdfs:isDefinedBy ?defined_by_uri . }
      		OPTIONAL { ?uri prov:wasDerivedFrom ?derived_from_uri . }
        }
      """)
  }

  def parse_publishedBy(): String = ""
  def parse_owner(): String = ""

  def parse_langs(): Seq[String] = {
    sparql.query("""
      SELECT DISTINCT ?lang 
      WHERE {  ?uri a owl:Ontology . ?uri rdfs:comment ?comment . BIND(LANG(?comment) AS ?lang) }
    """)
      .map(_.get("lang").getOrElse("").asInstanceOf[String])
      .distinct
  }

  def parse_lastEditDate(): String = {
    sparql.query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        SELECT DISTINCT ?last_edit_date {  
          ?uri a owl:Ontology . ?uri dct:modified ?date_modified .
        }
      """)
      .map(_.getOrElse("last_edit_date", "").asInstanceOf[String])
      .headOption.getOrElse("")
  }

  def parse_licenses(): Seq[URIWithLabel] = {
    sparql.query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        SELECT DISTINCT ?license_uri {  
          ?uri a owl:Ontology . ?uri dct:license ?license_uri .
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

  // should we shutdown the repository after using it?
  if (!repo.isInitialized()) repo.initialize()
}
