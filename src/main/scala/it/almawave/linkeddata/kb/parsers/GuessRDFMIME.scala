package it.almawave.linkeddata.kb.parsers

import org.eclipse.rdf4j.rio.RDFFormat
import java.net.URL
import org.eclipse.rdf4j.rio.Rio
import scala.util.Try
import scala.util.Success
import scala.util.Failure

/**
 * A simple facility class for detecting MIME type of a remote URL,
 * trying to use the standard RDF4J parsers.
 */
object GuessRDFMIME {

  def guess(url: URL): Try[RDFFormat] = {

    val guessed = Array(RDFFormat.TURTLE, RDFFormat.JSONLD, RDFFormat.RDFXML, RDFFormat.NTRIPLES, RDFFormat.N3)
      .map { format =>
        Try {
          val input = url.openStream()
          val test = Rio.parse(input, "", format)
          input.close()
          format
        }
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