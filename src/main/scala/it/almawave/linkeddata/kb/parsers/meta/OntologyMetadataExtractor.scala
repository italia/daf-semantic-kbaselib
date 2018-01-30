package it.almawave.linkeddata.kb.parsers.meta

import java.net.URL
import java.util.Properties

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import org.eclipse.rdf4j.common.iteration.Iterations
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.eclipse.rdf4j.sail.config.SailRegistry
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.lucene.LuceneSail
import org.eclipse.rdf4j.sail.solr.SolrIndex

import it.almawave.linkeddata.kb.catalog.models.RDFData
import it.almawave.linkeddata.kb.catalog.models.OntologyMeta
import it.almawave.linkeddata.kb.catalog.models.OntologyInformation
import it.almawave.linkeddata.kb.catalog.SPARQL
import it.almawave.linkeddata.kb.file.RDFFileRepository
import it.almawave.linkeddata.kb.catalog.models.URIWithLabel
import java.net.URI
import it.almawave.linkeddata.kb.catalog.models.ItemByLanguage
import it.almawave.linkeddata.kb.catalog.models.Version
import it.almawave.linkeddata.kb.catalog.models.LANG
import it.almawave.linkeddata.kb.utils.DateHelper
import it.almawave.linkeddata.kb.catalog.models.AssetType

/**
 * This is a simple helper object designed to extract as much information as possible from a single ontology.
 * The idea is to extract all the informations once for all.
 *
 * CHECK: for handling language-based literals, we could introduce a custom case class
 * (which will be later exposed as a clearer swagger model, too)
 *
 */
object OntologyMetadataExtractor {

  // REVIEW: this could be helpful in som cases for CLI
  def apply(source_url: URL): OntologyInformation = {
    val repo: Repository = new RDFFileRepository(source_url)
    if (!repo.isInitialized()) repo.initialize()
    val info = apply(source_url, repo)
    if (repo.isInitialized()) repo.shutDown()
    info
  }

  // REFACTORIZATION here! CHECK possible different storage for repository
  def apply(source_url: URL, repo: Repository): OntologyInformation =
    new OntologyMetadataExtractor(source_url, repo).informations()

}

class OntologyMetadataExtractor(source_url: URL, repo: Repository) {

  val sparql = SPARQL(repo)

  // CHECK: this is an experiment... SEE: RDFFrame class
  // TODO: etichette dei concetti
  lazy val frames = sparql.query("""
      SELECT DISTINCT ?super ?concept ?property 
      WHERE { 
        ?concept a ?klass . ?klass rdfs:subClassOf* ?super . 
        ?concept ?property [] .
      }
      ORDER BY ?super ?concept ?property
      """)
    .map { item => (item.get("concept").get.asInstanceOf[String], item.get("property").get.asInstanceOf[String]) }
    .groupBy { item => item._1 }
    .map { item => (item._1, item._2.toList.map(_._2)) }.toMap

  // useful for testing
  def parseData() = {

    println(s"getting basic informations for ${source_url}")
    val conn = repo.getConnection
    val contextsIDS = Iterations.asList(conn.getContextIDs)
    val subjects = Iterations.asList(conn.getStatements(null, null, null, true)).toStream.map { st => st.getSubject }.distinct.toSet
    val properties = Iterations.asList(conn.getStatements(null, null, null, true)).toStream.map { st => st.getPredicate }.distinct.toSet
    val objects = Iterations.asList(conn.getStatements(null, null, null, true)).toStream.map { st => st.getObject }.distinct.toSet
    val contexts = Iterations.asList(conn.getStatements(null, null, null, true)).toStream.map { st => st.getContext }.distinct.toSet
    conn.close()

    RDFData(subjects, properties, objects, contexts ++ contextsIDS)
  }

  def parseMeta(): OntologyMeta = {

    val source: URL = source_url

    val id: String = source.getPath.replaceAll(".*/(.*)\\.[a-z]+$", "$1").trim()
    val prefix: String = id.replaceAll("_", "").replaceAll("-", "").toLowerCase()

    val namespace: String = sparql.query("""
      SELECT DISTINCT ?uri { ?uri a owl:Ontology . } 
    """)
      .map(_.getOrElse("uri", source_url.toString()).asInstanceOf[String])
      .map { _.replaceAll("^(.*)[/#]$", "$1") } // hack for avoid using both `http://example/` and `http://example`
      .distinct
      .toSet.head
      .toString()

    val _onto_url = sparql.query("""
        SELECT DISTINCT ?uri 
        WHERE { ?onto_uri rdf:type owl:Ontology ; rdfs:isDefinedBy ?uri . }
      """)

    val onto_url = if (_onto_url.isEmpty)
      source
    else
      new URL(_onto_url(0)("uri").asInstanceOf[String].replaceAll("(.*)[#/]", "$1"))

    val concepts: Set[String] = sparql.query("""
        SELECT DISTINCT ?concept 
        WHERE { ?concept a owl:Class . FILTER(!isBlank(?concept)) } 
      """)
      .map(_.getOrElse("concept", "owl:Thing").toString()).toSet

    val imports: Seq[URIWithLabel] = sparql.query("""
        SELECT DISTINCT ?import_uri 
        WHERE {  ?uri a owl:Ontology . ?uri owl:imports ?import_uri . }
      """)
      .map(_.get("import_uri").get.asInstanceOf[String])
      .map { item => item.replaceAll("(.*)[#/]", "$1") }
      .map { item => URIWithLabel(item.replaceAll("^.*[#/](.*?)$", "$1"), item) }

    val titles: Map[String, String] = sparql.query("""
        SELECT DISTINCT * 
        WHERE {  ?uri a owl:Ontology . ?uri rdfs:label ?label . BIND(LANG(?label) AS ?lang) }
      """)
      .map { item =>
        (item.get("lang").get.asInstanceOf[String], item.get("label").get.asInstanceOf[String])
      }
      .toMap

    // TODO: case class | Map
    val descriptions: Map[String, String] = sparql.query("""
        SELECT DISTINCT * 
        WHERE {  ?uri a owl:Ontology . ?uri rdfs:comment ?comment . BIND(LANG(?comment) AS ?lang) }
      """)
      .map { item => (item.get("lang").get.asInstanceOf[String], item.get("comment").get.asInstanceOf[String]) }
      .toMap

    // TODO: case class
    val versions: Seq[Version] = sparql.query("""
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

    //    val creators: Seq[Map[String, String]] = sparql.query("""
    val creators: Seq[Map[String, String]] = sparql.query("""
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

    val provenance: Seq[Map[String, Any]] = sparql.query("""
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

    val langs: Seq[String] = sparql.query("""
      SELECT DISTINCT ?lang 
      WHERE {  ?uri a owl:Ontology . ?uri rdfs:comment ?comment . BIND(LANG(?comment) AS ?lang) }
    """)
      .map(_.get("lang").getOrElse("").asInstanceOf[String])
      .distinct

    val publishedBy: String = null
    val owner: String = null

    val lastEditDate: String = sparql.query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        SELECT DISTINCT ?last_edit_date {  
          ?uri a owl:Ontology . ?uri dct:modified ?date_modified .
        }
      """)
      .map(_.getOrElse("last_edit_date", "").asInstanceOf[String])
      .headOption.getOrElse("")

    val licenses: Seq[URIWithLabel] = sparql.query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        SELECT DISTINCT ?license_uri {  
          ?uri a owl:Ontology . ?uri dct:license ?license_uri .
        }
      """)
      .map(_.getOrElse("license_uri", "").asInstanceOf[String])
      .filterNot(_.equalsIgnoreCase(""))
      .map { item => URIWithLabel(item.replaceAll("^.*[#/](.*?)$", "$1"), item) }

    val tags: Seq[URIWithLabel] = List()
    val categories: Seq[URIWithLabel] = List()
    val keywords: Seq[String] = List()

    OntologyMeta(
      id,
      source,
      onto_url,
      prefix,
      namespace,
      concepts,
      imports,
      titles,
      descriptions,
      versions,
      creators,

      // CHECK with provenance
      publishedBy,
      owner,
      langs,
      lastEditDate,
      licenses,
      // CHECK with provenance

      tags,
      categories,
      keywords,

      provenance)

  }

  def informations() = {

    val data = parseData()
    val meta = parseMeta()

    OntologyInformation(meta, data)
  }

}

