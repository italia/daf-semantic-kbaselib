package it.almawave.linkeddata.kb.catalog

import it.almawave.linkeddata.kb.file.RDFFileRepository
import org.eclipse.rdf4j.repository.Repository
import java.net.URL
import org.slf4j.LoggerFactory
import it.almawave.linkeddata.kb.catalog.models.VocabularyMeta
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.memory.MemoryStore
import it.almawave.linkeddata.kb.catalog.models.URIWithLabel
import it.almawave.linkeddata.kb.catalog.models.ItemByLanguage
import it.almawave.linkeddata.kb.utils.URIHelper
import it.almawave.linkeddata.kb.catalog.models.Version
import it.almawave.linkeddata.kb.catalog.models.AssetType
import it.almawave.linkeddata.kb.parsers.VocabularyParser
import org.eclipse.rdf4j.sail.federation.Federation

object VocabularyBox {

  def parse(rdf_source: URL): VocabularyBox = {
    val parser = VocabularyParser(rdf_source)
    val meta = parser.parse_meta()
    new VocabularyBox(meta)
  }

}

class VocabularyBox(val meta: VocabularyMeta) extends RDFBox {

  override val id = meta.id
  override val context = meta.url.toString()

  override val repo: Repository = new RDFFileRepository(meta.source, context)

  // TODO: collecting sparql queries by configuration & convention

  //  val assetType = extract_assetType()

  override def toString() = s"""
    VocabularyBox [${id}, ${status}, ${triples} triples, ${context}]
  """.trim()

  // REVIEW
  def withImports() = {
    new VocabularyBoxWithImports(meta)
  }

  def federateWith(ontos: Seq[OntologyBox]) = {
    new VocabularyBoxWithDependencies(this, ontos)
  }

  /**
   * NOTE:	we assume we are modelling this vocabulary with a single specific representation technique (eg: SKOS)
   * ASK:		how can we can extend this this technique?
   */
  def extract_assetType() = {

    if (!repo.isInitialized()) repo.initialize()

    val reresentation_uri = SPARQL(repo).query("""
      PREFIX adms: <http://www.w3.org/ns/adms#> 
      SELECT ?representation 
      WHERE { 
        OPTIONAL { ?uri adms:representationTechnique ?representation . }
      } 
    """)
      .toList(0)
      .getOrElse("representation", "SKOS").asInstanceOf[String]

    val representaton_id = reresentation_uri.replaceAll(".*[#/](.*)", "$1")
    (representaton_id, reresentation_uri)

  }

}

class VocabularyBoxWithDependencies(vocab: VocabularyBox, ontos: Seq[OntologyBox])
  extends VocabularyBox(vocab.meta) {

  val federation = new Federation
  federation.addMember(vocab.repo)
  ontos.foreach(o => federation.addMember(o.repo))

  // TODO: extract prefixes from OntologyBox

  override val repo = new SailRepository(federation)

}

//class VocabularyBoxWithRepositories(meta: VocabularyMeta, base_repo: Repository) extends VocabularyBox(meta) {
//
//  // TODO: add dependencies in meta!
//  val _ontologies = meta.dependencies
//    .map { x => new URL(x) }.toList
//
//  val federation = new Federation()
//  federation.addMember(base_repo)
//
//  _ontologies.map { url =>
//
//    println("importing URL " + url)
//
//    //    val _repo = OntologyBox.parse(url).repo
//    //    federation.addMember(_repo)
//
//  }
//
//  override val repo: Repository = new SailRepository(federation)
//
//}

// creating the internal RDFFIleRepository from all the rdf sources
class VocabularyBoxWithImports(meta: VocabularyMeta) extends VocabularyBox(meta) {

  // TODO: add dependencies in meta!
  val _dependencies = meta.dependencies.map { x => new URL(x) }.toList

  override val repo: Repository = new RDFFileRepository(meta.source :: _dependencies, meta.url.toString())

}

object MainVocabularyBoxWithImports extends App {

  val voc = VocabularyBox.parse(new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/ClassificazioneTerritorio/Istat-Classificazione-08-Territorio.ttl"))
  //  voc.start()
  //
  //  SPARQL(voc.repo).query("""
  //    SELECT DISTINCT ?concept
  //    WHERE {
  //      ?s a ?concept .
  //    }
  //  """)
  //    .map(_.toMap.getOrElse("concept", "").toString())
  //    .map(_.replaceAll("^.*[#/](.*?)$", "$1"))
  //    .foreach { item =>
  //      println(item)
  //    }
  //  voc.stop()
  //
  //  val t1 = voc.triples
  //  println("triples: " + t1)
  //  println(voc.concepts.mkString(" | "))
  //
  //  println("\n...............................\n")

  println("DEP " + voc.meta.dependencies)

  val extra = voc.withImports()

  println("DEP " + extra.meta.dependencies)

  System.exit(0)
  extra.start()

  SPARQL(extra.repo).query("""
    SELECT DISTINCT ?concept 
    WHERE {
      ?s a ?concept .
    }
  """)
    .map(_.toMap.getOrElse("concept", "").toString())
    .map(_.replaceAll("^.*[#/](.*?)$", "$1"))
    .foreach { item =>
      println(item)
    }
  extra.stop()

  println(extra.concepts.mkString(" | "))
  val t2 = extra.triples
  println("triples: " + t2)

}



