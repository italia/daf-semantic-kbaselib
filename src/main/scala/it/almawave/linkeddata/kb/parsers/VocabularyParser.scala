package it.almawave.linkeddata.kb.parsers

import java.net.URL

import it.almawave.linkeddata.kb.catalog.SPARQL
import it.almawave.linkeddata.kb.catalog.models._
import it.almawave.linkeddata.kb.file.RDFFileRepository
import it.almawave.linkeddata.kb.utils.URIHelper
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer

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
  //  val sparql = SPARQL(repo)

  val id: String = rdf_source.getPath.replaceAll(".*/(.*)\\.[a-z]+$", "$1").trim()

  def parse_meta(): VocabularyMeta = {

    val voc_url = this.parse_voc_url()

    val instances = this.parse_instances()
    val titles = this.parse_titles()
    val descriptions = this.parse_descriptions()

    val creators = this.parse_creators()
    val owners = this.parse_owner()
    val publishedBy = this.parse_publishedBy()
    val langs = this.parse_langs()
    val licenses = this.parse_licenses()
    val versions = this.parse_versions()
    val creationDate = this.parse_creationDate()
    val lastEditDate = this.parse_lastEditDate()

    val tags: Seq[URIWithLabel] = this.parse_dcat_keywords()
    val themes: Seq[URIWithLabel] = this.parse_dcat_themes()
    val subthemes: Seq[URIWithLabel] = this.parse_dct_subthemes()

    val dependencies: Seq[String] = this.parse_dependencies()
    val hierarchy: ListBuffer[Hierarchy] = ListBuffer[Hierarchy]()
    val distributions: Seq[Distribution] = this.parse_dcat_distribution()

    VocabularyMeta(
      id,
      voc_url,
      rdf_source,
      instances,
      titles,
      descriptions,
      publishedBy,
      owners,
      creators,
      langs,
      licenses,
      versions,
      creationDate,
      lastEditDate,
      tags,
      themes,
      subthemes,
      dependencies,
      hierarchy,
      distributions)
  }

  /*
   * ASK: is it possibile to introduce a token class Vocabulary which is equivalent to Dataset?
   */
  def parse_voc_url(): URL = {
    SPARQL(repo).query("""
      PREFIX dcatapit: <http://dati.gov.it/onto/dcatapit#> 
      SELECT DISTINCT ?uri 
      WHERE { ?uri a dcatapit:Dataset . }
    """)
      .map { item => item.getOrElse("uri", "").asInstanceOf[String] }
      .map { item => new URL(item) }
      .headOption.getOrElse(rdf_source)
  }

  def parse_instances(): Set[String] = {
    SPARQL(repo).query("""
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      SELECT DISTINCT ?concept 
      WHERE { 
        { ?concept a ?klass. ?klass rdfs:subClassOf* skos:Concept . }
        UNION 
        { ?uri a ?concept . ?klass rdfs:subClassOf* owl:Class . } # IDEA: filter a specific set of classes here! 
      } 
      """)
      .map(_.getOrElse("concept", "owl:Thing").toString()).toSet
  }

  def parse_titles(): Seq[ItemByLanguage] = {
    SPARQL(repo).query(s"""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT * 
        WHERE { 
          ?uri a skos:ConceptScheme . 
          OPTIONAL { ?uri rdfs:label ?label . BIND(LANG(?label) AS ?lang) }
          OPTIONAL { ?uri dct:title ?label . BIND(LANG(?label) AS ?lang) } 
        }
        """)
      .map { item =>
        ItemByLanguage(item.getOrElse("lang", "").toString(), item.getOrElse("label", "").toString())
        // REFACTORIZATION: ItemByLanguage(item.getOrElse("lang", "").asInstanceOf[String], item.getOrElse("label", "").asInstanceOf[String])
      }
      .toList
  }

  def parse_descriptions(): Seq[ItemByLanguage] = {
    SPARQL(repo).query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT ?lang ?label  
        WHERE { 
          ?uri a skos:ConceptScheme . 
          OPTIONAL { ?uri rdfs:comment ?label . BIND(LANG(?label) AS ?lang) }
          OPTIONAL { ?uri dct:description ?label . BIND(LANG(?label) AS ?lang) } 
        }
        """)
      .map { item =>
        ItemByLanguage(item.getOrElse("lang", "").toString(), item.getOrElse("label", "").toString())
        // REFACTORIZATION: ItemByLanguage(item.getOrElse("lang", "").asInstanceOf[String], item.getOrElse("label", "").asInstanceOf[String])
      }
      .toList
  }

  def parse_publishedBy(): Seq[URIWithLabel] = {
//    SPARQL(repo).query("""
//        PREFIX dct: <http://purl.org/dc/terms/>
//        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
//        SELECT *
//        WHERE { ?uri a skos:ConceptScheme . ?uri dct:publisher ?publisher_uri .}
//      """)
//      // REFACTORIZATION: .map { item => item.getOrElse("publisher_uri", "").asInstanceOf[String] }
//      .map { item => item.getOrElse("publisher_uri", "").toString() }
//      .headOption.getOrElse("")

    var tags_container: Seq[URIWithLabel] = Seq[URIWithLabel]()
    var tags_container_tmp: Seq[URIWithLabel] = Seq[URIWithLabel]()

    val concepts = SPARQL(repo).query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX dcatapit: <http://dati.gov.it/onto/dcatapit#>
        PREFIX foaf: <http://xmlns.com/foaf/0.1/>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT ?publisher_uri
        WHERE {
          ?voc_uri a skos:ConceptScheme .
          ?voc_uri dct:publisher ?publisher_uri
        }
        """)

    concepts.foreach { concept =>

      val context = scala.collection.mutable.Map(concept.toSeq: _*).get("publisher_uri").get.toString
      tags_container_tmp = this.parse_detail(context)
      tags_container = tags_container_tmp.union(tags_container)
    }
    tags_container
  }

  def parse_creators(): Seq[URIWithLabel] = {
    var tags_container: Seq[URIWithLabel] = Seq[URIWithLabel]()
    var tags_container_tmp: Seq[URIWithLabel] = Seq[URIWithLabel]()

    val concepts = SPARQL(repo).query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX dcatapit: <http://dati.gov.it/onto/dcatapit#>
        PREFIX foaf: <http://xmlns.com/foaf/0.1/>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT ?creator_uri
        WHERE {
          ?voc_uri a skos:ConceptScheme .
          ?voc_uri dct:creator ?creator_uri
        }
        """)

    concepts.foreach { concept =>

      val context = scala.collection.mutable.Map(concept.toSeq: _*).get("creator_uri").get.toString
      tags_container_tmp = this.parse_detail(context)
      tags_container = tags_container_tmp.union(tags_container)
    }
    tags_container
  }

  def parse_owner(): Seq[URIWithLabel] = {
//    SPARQL(repo).query("""
//        PREFIX dct: <http://purl.org/dc/terms/>
//        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
//        SELECT *
//        WHERE { ?uri a skos:ConceptScheme . ?uri dct:rightsHolder ?holder_uri .}
//      """)
//      // REFACTORIZATION: .map { item => item.getOrElse("holder_uri", "").asInstanceOf[String] }
//      .map { item => item.getOrElse("holder_uri", "").toString() }
//      .headOption.getOrElse("")
    var tags_container: Seq[URIWithLabel] = Seq[URIWithLabel]()
    var tags_container_tmp: Seq[URIWithLabel] = Seq[URIWithLabel]()

    val concepts = SPARQL(repo).query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX dcatapit: <http://dati.gov.it/onto/dcatapit#>
        PREFIX foaf: <http://xmlns.com/foaf/0.1/>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT ?rightsHolder_uri
        WHERE {
          ?voc_uri a skos:ConceptScheme .
          ?voc_uri dct:rightsHolder ?rightsHolder_uri
        }
        """)

    concepts.foreach { concept =>

      val context = scala.collection.mutable.Map(concept.toSeq: _*).get("rightsHolder_uri").get.toString
      tags_container_tmp = this.parse_detail(context)
      tags_container = tags_container_tmp.union(tags_container)
    }
    tags_container
  }

  def parse_langs(): Seq[String] = {
    SPARQL(repo).query("""
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT ?lang  
        WHERE {
          ?uri a skos:ConceptScheme . 
          ?uri ?prp ?label . BIND(LANG(?label) AS ?lang) .
        }  
      """)
      // REFACTORIZATION: .map { item => item.getOrElse("lang", "").asInstanceOf[String] }
      .map { item => item.getOrElse("lang", "").toString() }
      .filterNot { item => item.trim().equalsIgnoreCase("") }
  }

  def parse_licenses(): Seq[URIWithLabel] = {
    SPARQL(repo).query("""
        PREFIX dcat: <http://www.w3.org/ns/dcat#>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        PREFIX dct: <http://purl.org/dc/terms/>
        SELECT DISTINCT ?license_uri 
        WHERE {
          ?uri a skos:ConceptScheme . 
          ?uri dcat:distribution ?distribution . ?distribution dct:license ?license_uri .
        }
      """)
      // REFACTORIZATION: .map(_.getOrElse("license_uri", "").asInstanceOf[String])
      .map(_.getOrElse("license_uri", "").toString())
      .filterNot(_.equalsIgnoreCase(""))
      .map { item =>
        val uri = item
        val label = URIHelper.extractLabelFromURI(uri)
        val lang = ""
        URIWithLabel(label, uri, lang)
      }
  }

  def parse_versions(): Seq[Version] = {
    SPARQL(repo).query("""
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT * 
        WHERE { 
          ?uri a skos:ConceptScheme . 
          ?uri owl:versionInfo ?version_info
          BIND(LANG(?version_info) AS ?lang)
        }
      """)
      // REFACTORIZATION: .map { item => item.getOrElse("version_info", "").asInstanceOf[String] }
      .map { item =>
        val lang = item.getOrElse("lang", "").toString()
//      }
//      .map { item =>
        val _matcher_number = "[0-9]+".r
        val _matcher = "^.*?(\\d+\\.\\d+(\\.\\d+)*).*\\s+-\\s+(.*)\\s+-\\s+(.*)$"
        val _vv = item.getOrElse("version_info", "").toString()
        val uri = item.getOrElse("uri", "").toString()
        if(_matcher_number.findFirstIn(_vv).nonEmpty) {
          var number = _vv
          Version(number, null, null, uri)
        }else {
          val _comment = _vv.replaceAll(_matcher, "$1")
          var comment = Map(lang -> _comment)
          Version(null, null, comment, uri)
      }
    }
  }

  def parse_creationDate() = {
    SPARQL(repo).query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT ?data_creation
        WHERE { ?uri a skos:ConceptScheme . ?uri dct:issued ?data_creation . }
      """)
      // REFACTORIZATION: .map { item => item.getOrElse("date_modified", "").asInstanceOf[String] }
      .map { item => item.getOrElse("data_creation", "").toString() }
      .headOption.getOrElse("")
  }

  def parse_lastEditDate() = {
    SPARQL(repo).query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT ?date_modified 
        WHERE { ?uri a skos:ConceptScheme . ?uri dct:modified ?date_modified . }  
      """)
      // REFACTORIZATION: .map { item => item.getOrElse("date_modified", "").asInstanceOf[String] }
      .map { item => item.getOrElse("date_modified", "").toString() }
      .headOption.getOrElse("")
  }

  def parse_dcat_keywords() = {
    SPARQL(repo).query("""
      PREFIX dcat: <http://www.w3.org/ns/dcat#>
      PREFIX dct: <http://purl.org/dc/terms/>
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      SELECT DISTINCT ?keyword ?lang 
      WHERE { ?uri a skos:ConceptScheme . ?uri dcat:keyword ?keyword . BIND(LANG(?keyword) AS ?lang) } 
    """)
      .map { item =>
        /*REFACTORIZATION
        val label = item.getOrElse("keyword", "").asInstanceOf[String].trim()
        val lang = item.getOrElse("lang", "").asInstanceOf[String]
        */
        val label = item.getOrElse("keyword", "").toString().trim()
        val lang = item.getOrElse("lang", "").toString().trim()
        val uri = s"keywords://${lang}#${label.replaceAll("\\s", "+")}"
        URIWithLabel(label, uri, lang)
      }
  }

  def parse_dcat_themes() = {
    SPARQL(repo).query("""
      PREFIX dcat: <http://www.w3.org/ns/dcat#>
      PREFIX dct: <http://purl.org/dc/terms/>
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      SELECT DISTINCT ?theme 
      WHERE { 
        ?uri a skos:ConceptScheme . ?uri dcat:theme ?theme .
      } 
    """)
      // REFACTORIZATION: .map { item => item.getOrElse("theme", "").asInstanceOf[String] }
      .map { item => item.getOrElse("theme", "").toString() }
      .map { item =>
        val uri = item
        val label = URIHelper.extractLabelFromURI(uri)
        val lang = "" // NOTE: can we assume dcat themes is defined for "eng" language?
        URIWithLabel(label, uri, lang)
      }
  }

  def parse_dct_subthemes() = {
    SPARQL(repo).query("""
      PREFIX dct: <http://purl.org/dc/terms/>
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      SELECT DISTINCT ?subject  
      WHERE { ?uri a skos:ConceptScheme . ?uri dct:subject ?subject . } 
    """)
      // REFACTORIZATION: .map { item => item.getOrElse("subject", "").asInstanceOf[String] }
      .map { item => item.getOrElse("subject", "").toString() }
      .map { item =>
        val uri = item
        val label = URIHelper.extractLabelFromURI(uri)
        val lang = ""
        URIWithLabel(label, uri, lang)
      }
  }

  def parse_dcat_distribution(): Seq[Distribution] = {

    var distribution_container: Seq[Distribution] = Seq[Distribution]()
    var distribution_container_tmp: Seq[Distribution] = Seq[Distribution]()

    val list = SPARQL(repo).query("""
     PREFIX dcat: <http://www.w3.org/ns/dcat#>
     PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
     SELECT DISTINCT ?distribution ?uri
     #FROM <test://accommodation-star-rating>
     WHERE {
       ?uri a skos:ConceptScheme .
       ?uri dcat:distribution ?distribution .
     }
    """)

      list.foreach { item =>
//        val distribution_uri = item
        distribution_container_tmp = this.parse_detail_dcat_distribution(item.get("distribution").get.toString)
        distribution_container = distribution_container_tmp.union(distribution_container)
      }
    distribution_container
  }

//  def parse_detail_dcat_distribution(distribution_uri: Map[String, Any]): Distribution = ???

  def parse_detail_dcat_distribution(distribution_uri : String) : Seq[Distribution] = {

    val list = SPARQL(repo).query(s"""
        PREFIX dcat: <http://www.w3.org/ns/dcat#>
        PREFIX dcatapit: <http://dati.gov.it/onto/dcatapit#>
        PREFIX dct: <http://purl.org/dc/terms/>
        SELECT DISTINCT *
        WHERE {
          ?distribution a dcatapit:Distribution .
          ?distribution dct:format ?format .
          ?distribution dct:license ?license .
          ?distribution dcat:downloadURL ?downloadURL .
          ?distribution dcat:accessURL ?accessURL .
          FILTER(?distribution=<$distribution_uri>) .
        }
      """)
      list.map { item =>
      val format = scala.collection.mutable.Map(item.toSeq: _*).get("format").get.toString
      val license = scala.collection.mutable.Map(item.toSeq: _*).get("license").get.toString
      val downloadURL = scala.collection.mutable.Map(item.toSeq: _*).get("downloadURL").get.toString
      val accessURL = scala.collection.mutable.Map(item.toSeq: _*).get("accessURL").get.toString
      val title = parse_detail_distribution_title(distribution_uri)//scala.collection.mutable.Map(item.toSeq: _*).get("title").get.toString
      val description = parse_detail_distribution_description(distribution_uri)//scala.collection.mutable.Map(item.toSeq: _*).get("description").get.toString

      Distribution(format, license, downloadURL, accessURL, title, description)
    }
  }

  def parse_detail_distribution_title(distribution_uri : String) : Seq[ItemByLanguage] = {
    val list = SPARQL(repo).query(s"""
        PREFIX dcat: <http://www.w3.org/ns/dcat#>
        PREFIX dcatapit: <http://dati.gov.it/onto/dcatapit#>
        PREFIX dct: <http://purl.org/dc/terms/>
        SELECT DISTINCT *
        WHERE {
          ?distribution a dcatapit:Distribution .
          {
          ?distribution dct:title ?title .
          BIND(LANG(?title) AS ?lang)
          }
          FILTER(?distribution=<$distribution_uri>) .
        }
      """)
    list.map { item =>

      ItemByLanguage(item.getOrElse("lang", "").toString(), item.getOrElse("title", "").toString())

    }
  }

  def parse_detail_distribution_description(distribution_uri : String) : Seq[ItemByLanguage] = {
    val list = SPARQL(repo).query(s"""
        PREFIX dcat: <http://www.w3.org/ns/dcat#>
        PREFIX dcatapit: <http://dati.gov.it/onto/dcatapit#>
        PREFIX dct: <http://purl.org/dc/terms/>
        SELECT DISTINCT *
        WHERE {
          ?distribution a dcatapit:Distribution .
          {
          ?distribution dct:description ?description .
          BIND(LANG(?description) AS ?lang)
          }
          FILTER(?distribution=<$distribution_uri>) .
        }
      """)
    list.map { item =>

      ItemByLanguage(item.getOrElse("lang", "").toString(), item.getOrElse("description", "").toString())

    }
  }

  def parse_dependencies(): Seq[String] = {

    SPARQL(repo).query("""
      SELECT DISTINCT ?concept
      WHERE {
        ?subject a ?concept
      }
    """)
      // REFACTORIZATION: .map(item => item.toMap.getOrElse("concept", "").asInstanceOf[String])
      .map(item => item.toMap.getOrElse("concept", "").toString())
      .map(item => item.replaceAll("^(.*)[#/].*?$", "$1").trim())
      .distinct

  }

  def parse_detail(context : String): Seq[URIWithLabel] = {

    SPARQL(repo).query(s"""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX dcatapit: <http://dati.gov.it/onto/dcatapit#>
        PREFIX foaf: <http://xmlns.com/foaf/0.1/>
        SELECT *
        WHERE {
          ?agent_uri a dcatapit:Agent .
          ?agent_uri foaf:name ?name .
          FILTER(?agent_uri=<$context>) .
          BIND(LANG(?name) AS ?name_lang)
        }
        """).map { item =>
      val uri = scala.collection.mutable.Map(item.toSeq: _*).get("agent_uri").get.toString
      val label = scala.collection.mutable.Map(item.toSeq: _*).get("name").get.toString
      val lang = scala.collection.mutable.Map(item.toSeq: _*).get("name_lang").get.toString
      URIWithLabel(label, uri, lang)
    }
  }

  // should we shutdown the repository after using it?
  if (!repo.isInitialized()) repo.initialize()
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

