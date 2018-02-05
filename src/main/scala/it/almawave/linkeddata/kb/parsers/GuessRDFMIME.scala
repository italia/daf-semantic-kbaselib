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

object MainGuessRDFMIME extends App {

  val url = new URL("http://dati.gov.it/onto/clvapit#")

  //  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/MappingTema-Sottotema/MappingDataThemeEurovoc.rdf")

  val ok = GuessRDFMIME.guess_format(url)

  println(ok)
}

/**
 * A simple facility class for detecting MIME type of a remote URL,
 * trying to use the standard RDF4J parsers.
 */
object GuessRDFMIME {

  def guess_format(url: URL): Try[RDFFormat] = {

    val found = Rio.getParserFormatForFileName(url.toString())

    if (found.isPresent()) {
      Success(found.get)

    } else {

      val guessed = Array(RDFFormat.RDFXML, RDFFormat.TURTLE, RDFFormat.JSONLD, RDFFormat.NTRIPLES, RDFFormat.N3)
        .map { format =>

          val tried = Try {
            val input = url.openStream()

            val vf = SimpleValueFactory.getInstance
            val parser = new ParserConfig

            val test = Rio.parse(input, "", format, parser, vf, null)

            input.close()
            format
          }

          println("\n" + format.getName + " :::: " + tried)

          tried

        }.toList
        .filterNot(_.isFailure)
        .map(_.get)

      if (guessed.size > 0) {
        Success(guessed(0))
      } else {
        val err = new RuntimeException(s"cannot detect the correct MIME format for ${url}")
        Failure(err)
      }
    }

  }

}