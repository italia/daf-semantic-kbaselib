package it.almawave.linkeddata.kb.file

import java.net.URL
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.sail.memory.MemoryStore
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import org.slf4j.LoggerFactory
import it.almawave.linkeddata.kb.parsers.GuessRDFMIME
import org.slf4j.LoggerFactory
import it.almawave.linkeddata.kb.utils.URLHelper
import scala.util.Success
import scala.util.Failure
import scala.util.Try

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

//  val _logger = LoggerFactory.getLogger(this.getClass)

  // REVIEW def this(url: URL, contexts: String*) = this(List(url), contexts: _*)

  val vf = SimpleValueFactory.getInstance
  val ctxs = if (contexts != null) contexts.map { cx => vf.createIRI(cx) } else Nil

  // CHECK: verify if vocabulary / ontology could have a configuration for baseURI
  val baseURI = if (contexts.size > 0) contexts.head else ""

  override def initialize() {
    super.initialize()
    this.load()
  }

  // SEE: memory usage
  // EX: http://www.javapractices.com/topic/TopicAction.do?Id=83

  private def load() {

    val conn = this.getConnection
    // IDEA: conn.begin()

    urls.foreach { url_src =>

      URLHelper.follow_redirect(url_src) match {

        case Success(url) => {

          logger.debug(s"\nloading RDF from url: <${url}>")

          try {
            val input = url.openStream()

            val guess = GuessRDFMIME.guess_format(url)

            if (guess.isSuccess) {

              // CHECK: val model = Rio.parse(input, baseURI, format, parser_config, vf, error_listener, ctxs: _*)
              val model = Rio.parse(input, baseURI, guess.get, ctxs: _*)

              conn.begin()
              model.unmodifiable()
                .toStream
                .foreach { st =>
                  // REVIEW: conn.addStatement(st.getSubject, st.getPredicate, st.getObject)
                  // REVIEW: conn.addStatement(st.getSubject, st.getPredicate, st.getObject, null)
                  conn.addStatement(st.getSubject, st.getPredicate, st.getObject, st.getContext)
                }
              conn.commit()

              logger.debug(s"added ${model.size()} triples from URL ${url}")

              input.close()

            } else {
              logger.error(s"cannot guess RDF format for URL ${url}")
            }

          } catch {
            case err: Throwable => logger.error(s"problem parsing ${url_src}!\n${err.getMessage}")
          }

        }

        case Failure(err) => {
          logger.error(s"problem parsing ${url_src}!\n${err.getMessage}")
        }

      }

    }

    // IDEA: conn.commit()

    conn.close()

  }

}
