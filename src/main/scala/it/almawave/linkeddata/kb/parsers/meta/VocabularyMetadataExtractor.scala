package it.almawave.linkeddata.kb.parsers.meta

import java.net.URL

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.common.iteration.Iterations

import it.almawave.linkeddata.kb.catalog.models.VocabularyMeta
import it.almawave.linkeddata.kb.catalog.models.VocabularyInformation
import it.almawave.linkeddata.kb.catalog.models.RDFData
import it.almawave.linkeddata.kb.catalog.SPARQL
import it.almawave.linkeddata.kb.file.RDFFileRepository
import it.almawave.linkeddata.kb.catalog.models.VocabularyMeta_NEW
import it.almawave.linkeddata.kb.catalog.models.URIWithLabel
import it.almawave.linkeddata.kb.catalog.models.AssetType

/**
 * This is a simple helper object designed to extract as much information as possible from a single vocabulary.
 * The idea is to extract all the informations once for all.
 */
object VocabularyMetadataExtractor {

  def apply(source_url: URL): VocabularyInformation = {
    val repo: Repository = new RDFFileRepository(source_url)
    apply(source_url, repo)
  }

  def apply(source_url: URL, repo: Repository): VocabularyInformation = {

    val repo: Repository = new RDFFileRepository(source_url)

    repo.initialize()

    val sparql = SPARQL(repo)

    // CHECK: this is an experiment... SEE: RDFFrame class
    val frames = sparql.query("""
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

    def parseMeta(): VocabularyMeta_NEW = {

      val source: URL = source_url

      val id: String = source_url.getPath.replaceAll(".*/(.*)\\.[a-z]+$", "$1").trim()

      val _voc_url = sparql.query("""
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        PREFIX adms: <http://www.w3.org/ns/adms#> 
        SELECT DISTINCT ?uri 
        WHERE { ?uri a adms:SemanticAsset }
      """)

      val voc_url = if (_voc_url.isEmpty) source else new URL(_voc_url(0)("uri").asInstanceOf[String])

      val instances: Set[String] = sparql.query("""
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT ?concept 
        WHERE { 
          ?concept a ?klass. 
          ?klass rdfs:subClassOf* skos:Concept . 
        } 
        """)
        .map(_.getOrElse("concept", "owl:Thing").toString()).toSet

      val titles: Map[String, String] = sparql.query(s"""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT * 
        WHERE { 
          ?uri a skos:ConceptScheme . 
          OPTIONAL { ?uri rdfs:label ?label . BIND(LANG(?label) AS ?lang) }
          OPTIONAL { ?uri dct:title ?label . BIND(LANG(?label) AS ?lang) } 
        }
        """)
        .map { item => (item.getOrElse("lang", "").asInstanceOf[String], item.getOrElse("label", "").asInstanceOf[String]) }
        .toMap

      // TODO: case class
      val descriptions: Map[String, String] = sparql.query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
        SELECT DISTINCT ?lang ?label  
        WHERE { 
          ?uri a skos:ConceptScheme . 
          OPTIONAL { ?uri rdfs:comment ?label . BIND(LANG(?label) AS ?lang) }
          OPTIONAL { ?uri dct:description ?label . BIND(LANG(?label) AS ?lang) } 
        }
        """)
        .map { item => (item.getOrElse("lang", "").asInstanceOf[String], item.getOrElse("label", "").asInstanceOf[String]) }
        .toMap

      // TODO: case class
      val version: Seq[(String, String)] = Seq.empty // TODO!

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

      val licenses: Seq[URIWithLabel] = List()
      val versions: Seq[(String, String)] = List()

      val tags: Seq[URIWithLabel] = List()
      val categories: Seq[URIWithLabel] = List()
      val keywords: Seq[String] = List()

      val lastEditDate: String = ""

      val asset = AssetType("taxonomy", "SKOS") // TODO: extract from vocabulary!!

      VocabularyMeta_NEW(
        id,
        source_url,
        source_url,
        instances,
        titles,
        descriptions,
        publishedBy, // TODO
        owner, // TODO
        creators, // TODO
        langs, // CHECK: LANG
        licenses,
        versions,
        lastEditDate,
        tags,
        categories,
        keywords)

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

      RDFData(subjects, properties, objects, contexts ++ contextsIDS)
    }

    val meta = parseMeta()
    val data = parseData()

    repo.shutDown()

    VocabularyInformation(meta, data)
  }

}
