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
    VocabularyBox [${id}, ${status}, ${triples} triples, ${context}] [${extract_assetType._1}]
  """.trim()

  // REVIEW
  //  def withImports() = {
  //    new VocabularyBoxWithImports(meta)
  //  }

  def federateWith(ontos: Seq[OntologyBox]) = {
    new VocabularyBoxWithDependencies(this, ontos)
  }

  // NOTE: this method is a workaround for missing representation_type
  def infer_vocabulary_type() = {
    SPARQL(repo).query("""
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      SELECT DISTINCT ?ontology_uri 
      WHERE {
        [] a ?concept . 
        OPTIONAL {
          ?concept a owl:Class .
        	?concept rdfs:isDefinedBy ?onto_uri .
          BIND(STR(?onto_uri) AS ?ontology_uri)
        }
        OPTIONAL {
          ?concept a* skos:Concept .
          BIND(STR(<http://www.w3.org/2004/02/skos/core#>) AS ?ontology_uri)
        }
      }
    """).toList
      .map(_.getOrElse("ontology_uri", "").toString())
      .filterNot(_.trim().equals("")).distinct
  }

  /**
   * NOTE:	we assume we are modelling this vocabulary with a single specific representation technique (eg: SKOS)
   * ASK:		how can we can extend this this technique?
   */
  def extract_assetType() = {

    val active = repo.isInitialized()
    if (!active) repo.initialize()

    def is_skos = SPARQL(repo).ask("""
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      ASK ?some a skos:Concept .
    """)

    /*
     * SEE example:
     * https://github.com/italia/daf-ontologie-vocabolari-controllati/blob/master/VocabolariControllati/Licenze/Licenze.ttl#L179-L180
     * 	+	dct:type <http://purl.org/adms/assettype/Taxonomy> ;
     * 	+	adms:representationTechnique <http://purl.org/adms/representationtechnique/SKOS>
     */
    val representation_uri = SPARQL(repo).query("""
        PREFIX dct: <http://purl.org/dc/terms/>
				PREFIX adms: <http://www.w3.org/ns/adms#> 
				SELECT ?representation_type ?representation_technique  
				WHERE { 
  				OPTIONAL { 
  				  ?uri dct:type ?representation_type .
  				  ?uri adms:representationTechnique ?representation_technique . 
  				}
				} 
		""")
      .toList(0)
      .getOrElse("representation_technique", "default:SKOS").asInstanceOf[String]

    val representaton_id = representation_uri.replaceAll(".*[#/](.*)", "$1")

    if (!active) repo.shutDown()

    (representaton_id, representation_uri)

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

// REVIEW
// creating the internal RDFFIleRepository from all the rdf sources
//class VocabularyBoxWithImports(meta: VocabularyMeta) extends VocabularyBox(meta) {
//
//  // TODO: add dependencies in meta!
//  val _dependencies = meta.dependencies.map { x => new URL(x) }.toList
//
//  override val repo: Repository = new RDFFileRepository(meta.source :: _dependencies, meta.url.toString())
//
//}
