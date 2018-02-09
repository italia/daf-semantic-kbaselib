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
import it.almawave.linkeddata.kb.catalog.models.RDFData_OLD
import it.almawave.linkeddata.kb.utils.JSONHelper
import org.eclipse.rdf4j.rio.Rio
import it.almawave.linkeddata.kb.repo.RDFRepository
import it.almawave.linkeddata.kb.parsers.GuessRDFMIME
import scala.util.Success
import scala.util.Failure

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
 */
@Deprecated
object MainOntologyRepositoryWrapper extends App {

  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/Organizzazioni/latest/COV-AP_IT.ttl")

  val onto = new OntologyRepositoryWrapper(url)
  // REVIEW .withDependencies()

  val info = onto.information.meta
  val json = JSONHelper.writeToString(info)
  println(json)

  println("ONTOLOGY: " + onto.ID)

  onto.entities.toList
    .foreach { item =>

      println("\n" + item._1)
      println("\t" + item._2.mkString("|"))

      val ref_daf = item._2.map { el => (item._1, el) }.map { el => s"${onto.ID}.${el._1}.${el._2}" }
      println("REF: ")
      println(ref_daf.mkString(" | "))

    }

}

// IDEA for refactoring...
class OntologyRepositoryWrapper(source_url: URL) {

  val ID = source_url.toString().replaceAll("^.*[#/](.*?)(\\.[a-z]*)*$", "$1")

  // REVIEW
  private var _repo: Repository = new RDFFileRepository(source_url)
  private val sparql = SPARQL(_repo)

  // REFACTORING: a method for embedding open/close repo as builder or side-effect
  // extracting general informations.......................................................
  if (!_repo.isInitialized()) _repo.initialize()

  private val _information = this.parse() // extract metadata
  val dependencies: Seq[String] = new URIDependenciesExtractor(_repo).extract().toList

  val entities = parse_entities

  //  if (_repo.isInitialized()) _repo.shutDown()
  // extracting general informations.......................................................

  private def parse(): OntologyInformation = OntologyMetadataExtractor(source_url, _repo).informations()

  def repository = _repo

  def information = _information

  // this is for extraction on concept.properties
  private def parse_entities = {

    SPARQL(_repo).query("""
      SELECT DISTINCT ?concept ?property 
      WHERE {
        ?concept a owl:Class .
        ?property rdfs:domain ?concept .
      }  
      ORDER BY ?concept ?property
    """)
      .map { item =>
        val concept = item.get("concept").get.toString().replaceAll("^.*[#/](.*)$", "$1")
        val property = item.get("property").get.toString().replaceAll("^.*[#/](.*)$", "$1")
        (concept, property)
      }
      .groupBy(_._1).map { el => (el._1, el._2.toList.map(_._2)) }

  }

  /**
   *  TODO: resolve & import dependencies
   *
   *  CHECK: since some of the public URL of the ontologies/vocabularies are not currently published,
   *  we should replace them with raw.github... source (temporarly)
   *
   */
  def withDependencies(): OntologyRepositoryWrapper = {

    if (!_repo.isInitialized()) _repo.initialize()

    val conn = _repo.getConnection
    conn.begin()
    dependencies.foreach { dep_url =>

      val url = new URL(dep_url)
      GuessRDFMIME.guess_format(url) match {
        case Success(mime) => conn.add(url, "", mime)
        case Failure(ok)   => println("ERROR: " + ok)
      }

      println("URL: " + url)

    }
    conn.commit()

    if (_repo.isInitialized()) _repo.shutDown()

    //    val urls = source_url :: dependencies.map(u => new URL(u)).toList
    //    _repo = new RDFFileRepository(urls)

    this
  }

}