package it.almawave.kb.catalog.meta

import java.net.URL

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import org.eclipse.rdf4j.common.iteration.Iterations
import org.eclipse.rdf4j.repository.Repository

import it.almawave.kb.catalog.SPARQL
import it.almawave.kb.catalog.file.RDFFileRepository
import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.eclipse.rdf4j.sail.config.SailRegistry
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.lucene.LuceneSail
import org.eclipse.rdf4j.sail.solr.SolrIndex
import java.util.Properties
import it.almawave.kb.catalog.models.RDFData
import it.almawave.kb.catalog.models.OntologyMeta
import it.almawave.kb.catalog.models.OntologyInformation

/**
 * This is a simple helper object designed to extract as much information as possible from a single ontology.
 * The idea is to extract all the informations once for all.
 * 
 * CHECK: for handling language-based literals, we could introduce a custom case class 
 * (which will be later exposed as a clearer swagger model, too)
 * 
 */
object OntologyMetadataExtractor {

  def apply(source_url: URL): OntologyInformation = {
    val repo: Repository = new RDFFileRepository(source_url)
    apply(source_url, repo)
  }

  // REFACTOR here! CHECK possible different storage for repository
  def apply(source_url: URL, repo: Repository): OntologyInformation = {

    if (!repo.isInitialized())
      repo.initialize()

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
      val onto_url = if (_onto_url.isEmpty) source else new URL(_onto_url(0)("uri").asInstanceOf[String].replaceAll("(.*)[#/]", "$1"))

      val concepts: Set[String] = sparql.query("""SELECT DISTINCT ?concept WHERE { ?concept a owl:Class . FILTER(!isBlank(?concept)) } """)
        .map(_.getOrElse("concept", "owl:Thing").toString()).toSet

      val imports: Set[String] = sparql.query("""SELECT DISTINCT ?import_uri {  ?uri a owl:Ontology . ?uri owl:imports ?import_uri . }""")
        .map(_.get("import_uri").get.asInstanceOf[String])
        .map { item => item.replaceAll("(.*)[#/]", "$1") }
        .toSet

      val titles: Seq[(String, String)] = sparql.query("""SELECT DISTINCT * {  ?uri a owl:Ontology . ?uri rdfs:label ?label . BIND(LANG(?label) AS ?lang) }""")
        .map { item => (item.get("lang").get.asInstanceOf[String], item.get("label").get.asInstanceOf[String]) }

      // TODO: case class
      val descriptions: Seq[(String, String)] = sparql.query("""SELECT DISTINCT * {  ?uri a owl:Ontology . ?uri rdfs:comment ?comment . BIND(LANG(?comment) AS ?lang) }""")
        .map { item => (item.get("lang").get.asInstanceOf[String], item.get("comment").get.asInstanceOf[String]) }

      // TODO: case class
      val version: Seq[(String, String)] = sparql.query("""SELECT DISTINCT * {  ?uri a owl:Ontology . 
        ?uri owl:versionIRI ?version_iri .
    		?uri owl:versionInfo ?version_info .
    		}""")
        .map { item => (item.get("version_iri").get.asInstanceOf[String], item.get("version_info").get.asInstanceOf[String]) }

      val creators: Set[String] = sparql.query("""
        PREFIX dc: <http://purl.org/dc/elements/1.1/>
        SELECT DISTINCT * { 
          ?uri a owl:Ontology . 
      		?uri dc:creator ?creator .
        }
        """)
        .map { item => item.get("creator").getOrElse(null).asInstanceOf[String] }
        .filterNot { _ == null }.toSet

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
        version,
        creators,
        provenance)

    }

    val data = parseData()
    val meta = parseMeta()

    if (repo.isInitialized())
      repo.shutDown()

    OntologyInformation(meta, data)
  }

}
