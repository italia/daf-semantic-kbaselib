package check

import java.net.URL
import examples.rdf4j.RDFFileSail
import it.almawave.linkeddata.kb.catalog.SPARQL
import it.almawave.linkeddata.kb.file.RDFFileRepository
import org.eclipse.rdf4j.query.QueryLanguage

object TestingTables extends App {

  val baseURI = "http://dati.gov.it/onto/"
  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/licences/licences.ttl")

  val repo = new RDFFileRepository(url)
  repo.initialize()

  val concepts = SPARQL(repo).query("""
  
    SELECT DISTINCT ?concept 
    WHERE { ?x a ?concept }  
    
  """)
    .map(_.getOrElse("concept", "").asInstanceOf[String])
    .filterNot(_.trim().equalsIgnoreCase(""))
    .filter(_.startsWith(baseURI))

  concepts.foreach { c =>

    println("concept: " + c)

  }

  var conn = repo.getConnection

  concepts.foreach { uri =>

    println("\n\n............................................................")
    println(s"\n\nDESCRIBE URI: <${uri}>")

    val prps = SPARQL(repo).query(s"""
    
      SELECT DISTINCT ?prp
      WHERE {
        <${uri}> ?prp [] .
      }   
      
    """)
      .map(_.getOrElse("prp", "").asInstanceOf[String])
      .filterNot(_.trim().equalsIgnoreCase(""))

    prps.foreach { p =>
      println("\t" + p)
    }

    val fields = prps.map { p =>
      val n = p.replaceAll(".*[#/](.*)", "$1")
      (n, "TEXT")
    }.map(x => s"${x._1} ${x._2}")

    val table_name = uri.toString().replaceAll(".*[#/](.*)", "$1")
    val sql_create = s"""
      DROP TABLE IF EXISTS `${table_name}`  ; 
      CREATE TABLE `${table_name}` (
        ${fields.mkString(",\n")}
      ) ;
      
    """

    //        NOTE: handle Class!

    println("SQL> " + sql_create.trim())

    val tuples = conn.prepareGraphQuery(QueryLanguage.SPARQL, s"""DESCRIBE <${uri}>""", baseURI).evaluate()

    println()
    while (tuples.hasNext()) {

      val st = tuples.next()

      val sub = st.getSubject
      val prp = st.getPredicate
      val obj = st.getObject
      val tbl = obj.toString().replaceAll(".*[#/](.*)", "$1")

      //      println("ST: " + st)

      prp match {

        case a if (a.toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) =>

          val sql = s"""
            INSERT INTO `${tbl}` (type) VALUES ( "${sub}" );
          """.trim()

          //          println("SQL> " + sql)
          println(sql)

        case _ =>
          println(prp)

      }
    }

  }

  conn.close()

}