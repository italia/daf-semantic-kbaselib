package examples.rdf4j.fts

import java.net.URL
import java.io.File
import org.eclipse.rdf4j.sail.lucene.LuceneSail
import examples.rdf4j.RDFFileSail
import org.eclipse.rdf4j.sail.solr.SolrIndex
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.rio.RDFFormat

object WorkingSolrExample extends App {

  //  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/Organizzazioni/latest/COV-AP_IT.ttl")
  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/IndirizziLuoghi/latest/CLV-AP_IT.ttl")
  //  val url = new File("/home/seralf/CLV-AP_IT.ttl").toURI().toURL()

  val DATA_DIR = new File("target/solr/data")

  val sail: LuceneSail = new LuceneSail()
  sail.setBaseSail(new RDFFileSail(url))
  sail.setParameter(LuceneSail.INDEX_CLASS_KEY, classOf[SolrIndex].getName())
  sail.setParameter(SolrIndex.SERVER_KEY, "embedded:")
  sail.setDataDir(DATA_DIR)

  val repo = new SailRepository(sail)
  repo.initialize()

  //  repo.getSail -> where a sail is accessible...
  //  something like: RespositorySail -> where only the repository interface is provided

  val conn = repo.getConnection
  conn.begin()
  conn.add(url, "", RDFFormat.TURTLE)
  conn.commit()

  sail.reindex()

  val query = """
    PREFIX search: <http://www.openrdf.org/contrib/lucenesail#> 
		SELECT ?subj ?snippet ?score
		WHERE { 
		  ?subj search:matches ?match .
		  ?match search:query "text:localita" .
		  ?match search:snippet ?snippet .
		  ?match search:score ?score . 
		}  
		ORDER BY DESC(?score)
  """

  val tuples = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()

  println("\nRESULTS....")
  while (tuples.hasNext()) {
    println(tuples.next())
  }
  println()

  conn.close()
  repo.shutDown()

  //  import org.apache.commons.io.FileUtils._
  //  FileUtils.deleteDirectory(DATA_DIR)

}