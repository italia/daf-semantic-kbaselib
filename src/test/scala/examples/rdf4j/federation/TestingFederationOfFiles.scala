package examples.rdf4j.federation

import org.eclipse.rdf4j.sail.federation.Federation
import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.repository.sail.SailRepository
import java.net.URL
import org.eclipse.rdf4j.query.QueryLanguage
import scala.collection.mutable.ListBuffer
import org.eclipse.rdf4j.query.BindingSet

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.sail.lucene.LuceneSail
import org.eclipse.rdf4j.sail.lucene.LuceneIndex

/**
 * this is a small POC to show how to use federation by convention over a collection of self-contained repository.
 *
 * TODO: check lucene sail!
 *
 */
object TestingFederationOfFiles extends App {

  val urls = List(
    "https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/ClassificazioneCategoriePuntoInteresse/POICategoryClassification.ttl",
    "https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/IndirizziLuoghi/latest/CLV-AP_IT.ttl",
    "https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/PuntoDiInteresse/latest/POI-AP_IT.ttl",
    "https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/Livello0/latest/l0.ttl")

  val federation = new Federation
  urls.foreach { url =>

    val context = "http://dati.gov.it/examples/" + url.toString().replaceAll(".*:/.*[#/](.*)\\..*", "$1")

    val urlSail = new RDFFileSail(url, context)

    // testing ith lucene.................................................
    //    val lucenesail = new LuceneSail()
    //    lucenesail.setParameter(LuceneSail.INDEX_CLASS_KEY, classOf[LuceneIndex].getName)
    //    lucenesail.setParameter(LuceneSail.LUCENE_RAMDIR_KEY, "true")
    //    lucenesail.setParameter(LuceneSail.LUCENE_DIR_KEY, "lucene")
    //    lucenesail.setBaseSail(urlSail)
    //    val lucrepo = new SailRepository(lucenesail)
    //    lucrepo.initialize()
    //    
    //    val cc = lucrepo.getConnection
    //    val vf = cc.getValueFactory
    //    val format = Rio.getParserFormatForFileName(url.toString()).get
    //    val source = new URL(url).openStream()
    //    val ctx = vf.createIRI(context)
    //    val rdf_model = Rio.parse(source, context, format, ctx)
    //    source.close()
    //    rdf_model.foreach { st =>
    //      cc.add(st, ctx)
    //    }
    //    cc.close()
    //
    //    federation.addMember(lucrepo)
    // testing ith lucene.................................................

    federation.addMember(new SailRepository(urlSail))
  }

  val repo = new SailRepository(federation)
  repo.initialize()

  val conn = repo.getConnection

  val query2 = """
    PREFIX search: <http://www.openrdf.org/contrib/lucenesail#> 
    SELECT ?subj ?text 
    WHERE { 
      ?subj search:matches [
        search:query ?term ; 
        search:snippet ?text 
      ] 
    }
  """

  val query = """
    
    PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>
    SELECT DISTINCT ?graph ?concept ?label ?text
    
    WHERE {
      GRAPH ?graph {
        
        ?concept a owl:Class .
        FILTER(!isBlank(?concept))
        
        ?concept rdfs:label ?label . 
        
        # ?label search:matches [ search:query "Luogo" ] .
        
      }
    }
    
    ORDER BY ?graph ?concept 
    # LIMIT 100
    
  """

  val tuples = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()

  val results = new ListBuffer[BindingSet]
  while (tuples.hasNext())
    results += tuples.next()

  results.toStream.foreach { item => println(item) }

  conn.close()

  repo.shutDown()

}

class RDFFileSail(source: String, context: String) extends MemoryStore {

  val url = new URL(source)

  override def initialize() = {
    super.initialize()
    load()
  }

  def load() {

    val cc = this.getConnection
    val vf = this.getValueFactory
    val format = Rio.getParserFormatForFileName(source).get
    val ctx = vf.createIRI(context)
    val input = url.openStream()
    val model = Rio.parse(input, context, format, ctx)
    input.close()
    cc.begin()
    model.foreach { st =>
      cc.addStatement(st.getSubject, st.getPredicate, st.getObject, st.getContext)
    }
    cc.commit()
    cc.close()

  }

}