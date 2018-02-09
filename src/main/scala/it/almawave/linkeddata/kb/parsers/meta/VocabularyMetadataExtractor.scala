package it.almawave.linkeddata.kb.parsers.meta

import java.net.URL

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.common.iteration.Iterations

import it.almawave.linkeddata.kb.catalog.models.VocabularyInformation
import it.almawave.linkeddata.kb.catalog.models.RDFData_OLD
import it.almawave.linkeddata.kb.catalog.SPARQL
import it.almawave.linkeddata.kb.file.RDFFileRepository
import it.almawave.linkeddata.kb.catalog.models.VocabularyMeta
import it.almawave.linkeddata.kb.catalog.models.URIWithLabel
import it.almawave.linkeddata.kb.catalog.models.AssetType
import it.almawave.linkeddata.kb.catalog.models.Version
import it.almawave.linkeddata.kb.utils.DateHelper
import java.net.URI
import it.almawave.linkeddata.kb.utils.URIHelper
import it.almawave.linkeddata.kb.catalog.models.ItemByLanguage

/**
 * This is a simple helper object designed to extract as much information as possible from a single vocabulary.
 * The idea is to extract all the informations once for all.
 */
object VocabularyMetadataExtractor {

  def apply(source_url: URL): VocabularyInformation = {
    val repo: Repository = new RDFFileRepository(source_url)
    if (!repo.isInitialized()) repo.initialize()
    val info = apply(source_url, repo)
    if (repo.isInitialized()) repo.shutDown()
    info
  }

  def apply(source_url: URL, repo: Repository): VocabularyInformation =
    new VocabularyMetadataExtractor(source_url, repo).informations()

}

@Deprecated
class VocabularyMetadataExtractor(source_url: URL, repo: Repository) {

  val sparql = SPARQL(repo)

  def parseMeta(): VocabularyMeta = {

    val id: String = source_url.getPath.replaceAll(".*/(.*)\\.[a-z]+$", "$1").trim()

    val _voc_url = sparql.query("""
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        PREFIX adms: <http://www.w3.org/ns/adms#> 
        SELECT DISTINCT ?uri 
        WHERE { ?uri a adms:SemanticAsset }
      """)

    val voc_url = if (_voc_url.isEmpty) source_url else new URL(_voc_url(0)("uri").asInstanceOf[String])

    val instances: Set[String] = sparql.query("""
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT ?concept 
        WHERE { 
          ?concept a ?klass. 
          ?klass rdfs:subClassOf* skos:Concept . 
        } 
        """)
      .map(_.getOrElse("concept", "owl:Thing").toString()).toSet

    val titles: Seq[ItemByLanguage] = sparql.query(s"""
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

    // TODO: case class
    val descriptions: Seq[ItemByLanguage] = sparql.query("""
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

    val creators: Seq[Map[String, String]] = List() // TODO!

    // TODO: we should add the parsing of `DCAT-AP_IT` for the vocabulary threaten as a dataset.

    val publishedBy: String = sparql.query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT * 
        WHERE { ?uri a skos:ConceptScheme . ?uri dct:publisher ?publisher_uri .}  
      """)
      .map { item => item.getOrElse("publisher_uri", "").asInstanceOf[String] }
      .headOption.getOrElse("")

    val owner: String = sparql.query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT * 
        WHERE { ?uri a skos:ConceptScheme . ?uri dct:rightsHolder ?holder_uri .}  
      """)
      .map { item => item.getOrElse("holder_uri", "").asInstanceOf[String] }
      .headOption.getOrElse("")

    val langs: Seq[String] = sparql.query("""
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT ?lang  
        WHERE {
          ?uri a skos:ConceptScheme . 
          ?uri ?prp ?label . BIND(LANG(?label) AS ?lang) .
        }  
      """)
      .map { item => item.getOrElse("lang", "").asInstanceOf[String] }
      .filterNot { item => item.trim().equalsIgnoreCase("") }

    val licenses: Seq[URIWithLabel] = sparql.query("""
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

    // TODO: case class
    val versions: Seq[Version] = sparql.query("""
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

    //      dcat:keyword "Punti di Interesse"@it, "Categoria"@it , "Settore"@it, "Point of Interest"@en, "Category"@en , "Sector"@en ;

    val dcat_keywords = sparql.query("""
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

    val dcat_themes = sparql.query("""
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

    val dc_subjects = sparql.query("""
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

    //REVIEW here
    val tags: Seq[URIWithLabel] = dc_subjects
    val categories: Seq[URIWithLabel] = dcat_themes
    val keywords: Seq[URIWithLabel] = dcat_keywords

    val lastEditDate: String = sparql.query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT ?date_modified 
        WHERE { ?uri a skos:ConceptScheme . ?uri dct:modified ?date_modified . }  
      """)
      .map { item => item.getOrElse("date_modified", "").asInstanceOf[String] }
      .headOption.getOrElse("")

    val asset = AssetType("taxonomy", "SKOS") // TODO: extract from vocabulary!!

    val url: URL = sparql.query("""
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT ?uri 
        WHERE { ?uri a skos:ConceptScheme . }
      """)
      .map { item => item.getOrElse("uri", "").asInstanceOf[String] }
      .map { item => new URL(item) }
      .headOption.getOrElse(null)

    val dependencies: Seq[String] = List()

    VocabularyMeta(
      id,
      url,
      source_url,
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

    RDFData_OLD(subjects, properties, objects, contexts ++ contextsIDS)
  }

  def informations() = {

    if (!repo.isInitialized()) repo.initialize()

    val data = parseData()
    val meta = parseMeta()

    VocabularyInformation(meta, data)
  }

}
