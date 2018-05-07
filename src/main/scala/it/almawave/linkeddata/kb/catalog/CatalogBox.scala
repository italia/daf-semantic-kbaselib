package it.almawave.linkeddata.kb.catalog

import com.typesafe.config.Config

import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.memory.MemoryStore
import scala.collection.mutable.ListBuffer
import java.net.URI
import java.nio.file.Paths
import java.net.URL
import org.eclipse.rdf4j.sail.inferencer.fc.ForwardChainingRDFSInferencer
import org.eclipse.rdf4j.sail.inferencer.fc.DedupingInferencer
import org.eclipse.rdf4j.common.iteration.Iterations
import org.eclipse.rdf4j.sail.federation.Federation
import org.eclipse.rdf4j.sail.Sail
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import java.io.File
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.util.FileUtils
import com.typesafe.config.ConfigFactory
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import org.eclipse.jgit.merge.MergeStrategy

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import org.slf4j.LoggerFactory

/*
 * TODO: consider using inferences
 */
class CatalogBox(config: Config) extends RDFBox {

  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

  val federation = new Federation()

  override val repo: Repository = new SailRepository(federation)

  override val conf = config.resolve()

  private val _ontologies = new ListBuffer[OntologyBox]
  private val _vocabularies = new ListBuffer[VocabularyBox]
  private val _remotes = new ListBuffer[OntologyBox]

  def ontologies: Seq[OntologyBox] = _ontologies.toList
  def vocabularies: Seq[VocabularyBox] = _vocabularies.toList
  def externalOntologies: Seq[OntologyBox] = _remotes.toList

  // handles local copies of RDF files
  val store = RDFFilesStore(conf.getString("ontologies.path_local"))

  // handles the lightweight git client
  val git = GitHandler(conf.getConfig("git"))

  override def start() {

    // synchronize with remote git repository
    if (conf.getBoolean("git.synchronize")) git.synchronize()

    if (!repo.isInitialized()) repo.initialize()

    // load ontologies!
    this.load_ontologies

    // load vocabularies!
    this.load_vocabularies

//    TODO Commentato per il momento non viene usato
    this.load_remotes

    // starts the different kbboxes
    (_ontologies ++ _vocabularies ++ _remotes) foreach (_.start())

    // adding triples to global federated repository
    _ontologies.foreach { x => federation.addMember(x.repo) }
    _vocabularies.foreach { x => federation.addMember(x.repo) }
    //    _remotes.foreach { x => federation.addMember(x.repo) }

  }

  override def stop() {

    // starts the different kbboxes
    (_ontologies ++ _vocabularies ++ _remotes) foreach (_.stop())

    if (repo.isInitialized()) repo.shutDown()
  }

  override def triples = {
    _ontologies.foldLeft(0)((a, b) => a + b.triples) + _vocabularies.foldLeft(0)((a, b) => a + b.triples)
  }

  def getVocabularyByID(vocabularyID: String) = Try {
    this._vocabularies.toStream.filter(_.id.equals(vocabularyID)).head
  }

  private def load_remotes = {

    // TODO: add SKOS as a local copy (cache), and use it
    // IDEA: loading pre-defined ontologies (eg: SKOS)
    val skos_box = RemoteOntologyBox.parse(
      new URL("http://www.w3.org/2004/02/skos/core#"),
      new URL("http://www.w3.org/TR/skos-reference/skos.rdf"))
    this._remotes += skos_box

  }

  private def load_ontologies = {

    val base_path: URI = if (conf.getBoolean("ontologies.use_cache"))
      Paths.get(conf.getString("ontologies.path_local")).normalize().toAbsolutePath().toUri()
    else
      new URI(conf.getString("ontologies.path_remote"))

    if (conf.hasPath("ontologies.data")) {

      logger.info(s"using selected ontologies")

      conf.getConfigList("ontologies.data")
        .toStream
        .foreach { onto_conf =>
          val source_path = onto_conf.getString("path")
          val source_url = new URI(base_path + source_path).normalize().toURL()
          val box = OntologyBox.parse(source_url)
          box.start()
          box.stop()
          _ontologies += box
        }

    } else {

      logger.info(s"using all available ontologies")

      store.ontologies().foreach { f =>

        val box = OntologyBox.parse(f.toURI().toURL())
        box.start()
        box.stop()
        _ontologies += box
      }

    }

    _ontologies.toStream

  }

  // TODO: same for vocabulary?
  private def getOntologyBoxByContext(context: String) = Try {
    _ontologies.toStream.filter(_.context.equals(context)).head
  }

  private def load_vocabularies = {

    val base_path: URI = if (conf.getBoolean("vocabularies.use_cache"))
      Paths.get(conf.getString("vocabularies.path_local")).normalize().toAbsolutePath().toUri()
    else
      new URI(conf.getString("vocabularies.path_remote"))

    if (conf.hasPath("vocabularies.data")) {

      logger.info(s"using selected vocabularies")

      conf.getConfigList("vocabularies.data")
        .toStream
        .foreach { voc_conf =>

          val source_path = voc_conf.getString("path")
          val source_url = new URI(base_path + source_path).normalize().toURL()
          val box = VocabularyBox.parse(source_url)
          box.start()
          box.stop()
          _vocabularies += box
        }

    } else {

      logger.info(s"using all available vocabularies")

      store.vocabularies().foreach { f =>
        val box = VocabularyBox.parse(f.toURI().toURL())
        box.start()
        box.stop()
        _vocabularies += box
      }

    }

    _vocabularies.toStream

  }

  def resolveVocabularyDependencies(vbox: VocabularyBox): VocabularyBox = {

    val ontos_uris = vbox.infer_ontologies()

    val boxes_int = ontos_uris.flatMap { onto_uri => this.ontologies.find(_.context.equals(onto_uri)) }
    val boxes_ext = ontos_uris.flatMap { onto_uri => this.externalOntologies.find(_.context.equals(onto_uri)) }

    vbox.federateWith(boxes_ext)

  }

  /*
   * REVIEW the resolution of triples from dependencies
   *
   * TODO: add JUnit
   */
  def vocabulariesWithDependencies(): Seq[VocabularyBox] = {

    this.vocabularies.map { vbox =>

      // getting the vocabulary id
      val vocID = vbox.id

      // finding vocabulary by id
      var voc_box = this.getVocabularyByID(vocID).get

      val triples_no_deps = voc_box.triples

      // TODO: resolve internal dependencies
      // TODO: federation with dependencies

      val ontos = voc_box.infer_ontologies()

      val triples_deps = voc_box.triples

      voc_box
    }

  }

  /**
   * this method should return a list containing only the actually used ontologies (in the network)
   */
  private def get_internal_dependencies(voc_box: VocabularyBox): Seq[String] = {

    // TODO: extend to multiple baseURI

    val uris = this.conf.getStringList("ontologies.baseURI")

    voc_box.meta.dependencies.toStream
      .filter { d =>
        uris.filter { u => d.startsWith(u) }.size == 1
      }.toList

  }

  /**
   * this method should add the dependencies for ontologies used in the vocabulary
   */
  @Deprecated
  private def resolve_dependencies(voc_box: VocabularyBox): Seq[OntologyBox] = {

    // list of dependencies
    val deps = this.get_internal_dependencies(voc_box).map(new URL(_))
    //    logger.debug(s"\ninternal dependencies for ${voc_box} are:\n\t${deps.mkString("\n\t")}")

    // boxes found
    //    this._remotes.toList ++
    deps.flatMap { dep_ctx =>
      this.ontologies.toList
        .map { ctx => ctx }
        .filter(_.context.trim().equals(dep_ctx.toString().trim()))
    }

  }

  /*
   * REVIEW: retrieves an OntologyBox by its uri (this is useful for federating a VocabularyBox with its dependencies)
   */
  def getOntologyByURI(uri: String): Try[OntologyBox] = Try {
    this._ontologies.filter(_.context.toString().equals(uri)).head
  }

}
