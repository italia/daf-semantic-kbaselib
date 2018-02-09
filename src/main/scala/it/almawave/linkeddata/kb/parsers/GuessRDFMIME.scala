package it.almawave.linkeddata.kb.parsers

import org.eclipse.rdf4j.rio.RDFFormat
import java.net.URL
import org.eclipse.rdf4j.rio.Rio
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.rio.ParserConfig
import org.eclipse.rdf4j.rio.RioSetting
import org.eclipse.rdf4j.rio.RioConfig
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings
import java.util.Scanner
import scala.io.Source
import java.net.HttpURLConnection
import java.io.File
import java.io.StringReader
import org.eclipse.rdf4j.rio.ParseErrorListener
import java.io.ByteArrayInputStream
import org.slf4j.LoggerFactory

object MainGuessRDFMIME extends App {

  //  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/MappingTema-Sottotema/MappingDataThemeEurovoc.rdf")

  val url = new URL("http://dati.gov.it/onto/clvapit#")
  //    val url = new File("D:/CLV-AP_IT_301").toURI().toURL()
  //  val url = new File("D:/CLV-AP_IT_protege").toURI().toURL()
  //      val url = new File("D:/CLV-AP_IT").toURI().toURL()

  val ok = GuessRDFMIME.guess_format(url)
  println(ok)

  //  CHECK: redirect
  //  val url = "http://bit.ly/23414"
  //  val conn = url.openConnection().asInstanceOf[HttpURLConnection]
  //  conn.setInstanceFollowRedirects(false)
  //  conn.connect()
  //  val url_real = new URL(conn.getHeaderField("Location"))
  //  println(url_real)

}

/**
 * A simple facility class for detecting MIME type of a remote URL,
 * trying to use the standard RDF4J parsers.
 */
object GuessRDFMIME {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def guess_format(url: URL): Try[RDFFormat] = {

    val found = Rio.getParserFormatForFileName(url.toString())

    if (found.isPresent()) {

      Success(found.get)

    } else {

      val guessed = Array(RDFFormat.RDFXML, RDFFormat.TURTLE, RDFFormat.JSONLD, RDFFormat.NTRIPLES, RDFFormat.N3)
        .map { format =>

          val tried = Try {
            val input = url.openStream()
            Rio.parse(input, "", format, new ParserConfig, SimpleValueFactory.getInstance, null)
            input.close()
            format
          }

          logger.debug(s"trying to parse ${url} as ${format.getName} format.")

          tried

        }.toList
        .filterNot(_.isFailure)
        .map(_.get)

      if (guessed.size > 0) {
        logger.info(s"parse ${url} as ${guessed(0)} format.")
        Success(guessed(0))
      } else {
        val err = new RuntimeException(s"cannot detect the correct MIME format for ${url}")
        Failure(err)
      }
    }

  }

}