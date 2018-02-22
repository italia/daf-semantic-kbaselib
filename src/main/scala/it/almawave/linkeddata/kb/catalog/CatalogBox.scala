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

class CatalogBox(config: Config) extends RDFBox {

  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

  val federation = new Federation()
  //  new ForwardChainingRDFSInferencer(new DedupingInferencer(federation))
  override val repo: Repository = new SailRepository(federation)

  override val conf = config.resolve()

  private val _ontologies = new ListBuffer[OntologyBox]
  private val _vocabularies = new ListBuffer[VocabularyBox]
  private val _remotes = new ListBuffer[RemoteOntologyBox]

  def ontologies = _ontologies.toList
  def vocabularies = _vocabularies.toList

  override def start() {

    if (!repo.isInitialized()) repo.initialize()

    // load ontologies!
    this.load_ontologies

    // load vocabularies!
    this.load_vocabularies

    // starts the different kbboxes
    (_ontologies ++ _vocabularies) foreach (_.start())

    // adding triples to global federated repository
    _ontologies.foreach { x => federation.addMember(x.repo) }
    _vocabularies.foreach { x => federation.addMember(x.repo) }
    _remotes.foreach { x => federation.addMember(x.repo) }

  }

  override def stop() {

    // starts the different kbboxes
    (_ontologies ++ _vocabularies) foreach (_.stop())

    if (repo.isInitialized()) repo.shutDown()
  }

  override def triples = {
    _ontologies.foldLeft(0)((a, b) => a + b.triples) +
      _vocabularies.foldLeft(0)((a, b) => a + b.triples)
  }

  def getVocabularyByID(vocabularyID: String) = Try {
    this._vocabularies.toStream.filter(_.id.equals(vocabularyID)).head
  }

  private def load_ontologies = {

    // TODO: re-add remote gathering, with jgit
    val base_path: URI = if (conf.getBoolean("ontologies.use_cache"))
      Paths.get(conf.getString("ontologies.path_local")).normalize().toAbsolutePath().toUri()
    else
      new URI(conf.getString("ontologies.path_remote"))

//    if(conf.hasPath("ontologies.data")) {}
    
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

    _ontologies.toStream

  }

  // TODO: same for vocabulary?
  private def getOntologyBoxByContext(context: String) = Try {
    _ontologies.toStream.filter(_.context.equals(context)).head
  }

  private def load_vocabularies = {

    // TODO: re-add remote gathering, with jgit
    val base_path: URI = if (conf.getBoolean("vocabularies.use_cache"))
      Paths.get(conf.getString("vocabularies.path_local")).normalize().toAbsolutePath().toUri()
    else
      new URI(conf.getString("vocabularies.path_remote"))

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

    _vocabularies.toStream

  }

  // CHECK: repository <all>

  def vocabulariesWithDependencies(): Seq[VocabularyBox] = {

    this.vocabularies.map { vbox =>

      // getting the vocabulary id
      val vocID = vbox.id

      // find vocabulary by id
      var voc_box = this.getVocabularyByID(vocID).get

      val triples_no_deps = voc_box.triples

      // resolve internal dependencies
      val ontos = this.resolve_dependencies(voc_box)

      // federation with repositories
      voc_box = voc_box.federateWith(ontos)

      val triples_deps = voc_box.triples

      voc_box
    }

  }

  // resolve only internal dependencies
  private def internal_dependencies(voc_box: VocabularyBox) = {
    val onto_baseURI = this.conf.getString("ontologies.baseURI").trim()
    voc_box.meta.dependencies.toStream.filter { d => d.startsWith(onto_baseURI) }
  }

  private def resolve_dependencies(voc_box: VocabularyBox): Seq[OntologyBox] = {

    // list of dependencies
    val deps = this.internal_dependencies(voc_box).map(new URL(_))

    // boxes found
    deps.flatMap { dep_ctx =>
      this.ontologies.toList
        .filter(_.context.trim().equals(dep_ctx.toString().trim()))
    }

  }

}
