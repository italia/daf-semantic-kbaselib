package it.almawave.linkeddata.kb.parsers.meta

import java.net.URL

import it.almawave.linkeddata.kb.file.RDFFileRepository
import it.almawave.linkeddata.kb.catalog.models.OntologyMeta
import it.almawave.linkeddata.kb.catalog.models.OntologyInformation
import it.almawave.linkeddata.kb.catalog.SPARQL

import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.common.iteration.Iterations

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import it.almawave.linkeddata.kb.catalog.models.RDFData
import it.almawave.linkeddata.kb.utils.JSONHelper

/**
 * REFACTORIZATION
 *
 * this is a first attempt to re-engineer the logic behind the extraction,
 * adding also an explicit external reference to the internal repository
 *
 * TODO: for each ontology/vocabulary, should be provided a configurable import of all the dependencies
 *
 * for ontologies: this will directly references the defined imports, as well as the dependencies expressed by prefixes/namespaces declaration
 * for vocabulary: this will reference the prefixes/namespaces declaration
 *
 *
 *
 */
object MainOntologyRepositoryWrapper extends App {

  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/Organizzazioni/latest/COV-AP_IT.ttl")

  val onto = new OntologyRepositoryWrapper(url)

  val info = onto.information.meta
  val json = JSONHelper.writeToString(info)
  println(json)

  val repo = onto.repository
  repo.initialize()

  val results = SPARQL(repo).query("""
    SELECT DISTINCT ?concept ?property 
    WHERE {
      ?concept a owl:Class .
      # ?concept owl:subclassOf* owl:Class .
      ?property rdfs:domain ?concept .
    }  
    ORDER BY ?concept ?property
  """)
    .map { item =>
      val concept = item.get("concept").get.toString().replaceAll("^.*[#/](.*)$", "$1")
      val property = item.get("property").get.toString().replaceAll("^.*[#/](.*)$", "$1")
      (concept, property)
    }

  results.toList
    .groupBy(_._1).map { el => (el._1, el._2.toList.map(_._2)) }
    .foreach { item =>

      println("\n" + item._1)
      println("\t" + item._2.mkString("|"))

      val ref_daf = item._2.map { el => (item._1, el) }.map { el => s"${onto.ID}.${el._1}.${el._2}" }
      println("REF: " + ref_daf)

    }

  println("ONTOLOGY: " + onto.ID)

  repo.shutDown()

}

// IDEA for refactoring...
class OntologyRepositoryWrapper(source_url: URL) {

  val ID = source_url.toString().replaceAll("^.*[#/](.*?)(\\.[a-z]*)*$", "$1")

  // REVIEW
  private val _repo: Repository = new RDFFileRepository(source_url)
  private val sparql = SPARQL(_repo)

  if (!_repo.isInitialized())
    _repo.initialize()

  private val _information = this.parse() // extract metadata

  if (_repo.isInitialized())
    _repo.shutDown()

  // REFACTORIZATION here! CHECK possible different storage for repository
  private def parse(): OntologyInformation = {

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
      val conn = _repo.getConnection
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

      val concepts: Set[String] = sparql.query("""
        SELECT DISTINCT ?concept 
        WHERE { ?concept a owl:Class . FILTER(!isBlank(?concept)) } 
      """)
        .map(_.getOrElse("concept", "owl:Thing").toString()).toSet

      val imports: Set[String] = sparql.query("""
        SELECT DISTINCT ?import_uri 
        WHERE {  ?uri a owl:Ontology . ?uri owl:imports ?import_uri . }
      """)
        .map(_.get("import_uri").get.asInstanceOf[String])
        .map { item => item.replaceAll("(.*)[#/]", "$1") }
        .toSet

      val titles: Seq[(String, String)] = sparql.query("""
        SELECT DISTINCT * 
        WHERE {  ?uri a owl:Ontology . ?uri rdfs:label ?label . BIND(LANG(?label) AS ?lang) }
      """)
        .map { item => (item.get("lang").get.asInstanceOf[String], item.get("label").get.asInstanceOf[String]) }

      // TODO: case class | Map
      val descriptions: Seq[(String, String)] = sparql.query("""
        SELECT DISTINCT * 
        WHERE {  ?uri a owl:Ontology . ?uri rdfs:comment ?comment . BIND(LANG(?comment) AS ?lang) }
      """)
        .map { item => (item.get("lang").get.asInstanceOf[String], item.get("comment").get.asInstanceOf[String]) }

      // TODO: case class
      val version: Seq[(String, String)] = sparql.query("""
        SELECT DISTINCT * 
        WHERE {  
          ?uri a owl:Ontology . 
          ?uri owl:versionIRI ?version_iri .
      		?uri owl:versionInfo ?version_info .
    		}""")
        .map { item => (item.get("version_iri").get.asInstanceOf[String], item.get("version_info").get.asInstanceOf[String]) }

      val creators: Set[String] = sparql.query("""
        PREFIX dc: <http://purl.org/dc/elements/1.1/>
        SELECT DISTINCT * 
        WHERE { 
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

    OntologyInformation(meta, data)
  }

  def repository = _repo

  def information = _information

}