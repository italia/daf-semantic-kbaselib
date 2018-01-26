package it.almawave.linkeddata.kb.file

import java.net.URL
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.sail.memory.MemoryStore
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import org.slf4j.LoggerFactory

/**
 * examples:
 *
 * 1)
 * val urls: List[URL] = List(new URL("http://some/file.rdf"))
 * val contexts: List[String] = List("http://example/some/file.rdf")
 * val sail = val new RDFFileSail(urls, contexts:_*)
 *
 * ...
 *
 * 2)
 * val url = new URL("http://some/file.rdf")
 * val sail = val new RDFFileSail(url)
 *
 */
class RDFFileSail(urls: Seq[URL], contexts: String*) extends MemoryStore {

  def this(url: URL, contexts: String*) = this(List(url), contexts: _*)

  val vf = SimpleValueFactory.getInstance
  val ctxs = contexts.map { cx => vf.createIRI(cx) }

  val baseURI = if (contexts.size > 0) contexts.head else ""

  override def initialize() {
    super.initialize()
    load()
  }

  // SEE: memory usage
  // EX: http://www.javapractices.com/topic/TopicAction.do?Id=83

  private def load() {

    val conn = this.getConnection
    // IDEA: conn.begin()

    urls.foreach { url =>

      logger.debug(s"loading RDF from url: <${url}>")
      val input = url.openStream()

      val format: RDFFormat = Rio.getParserFormatForFileName(url.toString()).get

      // CHECK: val model = Rio.parse(input, baseURI, format, parser_config, vf, error_listener, ctxs: _*)
      val model = Rio.parse(input, baseURI, format, ctxs: _*)

      conn.begin()
      model.unmodifiable()
        .toStream
        .foreach { st =>
          conn.addStatement(st.getSubject, st.getPredicate, st.getObject, st.getContext)
        }
      conn.commit()

      logger.debug(s"added ${model.size()} triples from URL ${url}")

      input.close()

    }

    // IDEA: conn.commit()

    conn.close()

  }

}
