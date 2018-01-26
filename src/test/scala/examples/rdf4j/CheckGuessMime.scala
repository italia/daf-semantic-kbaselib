package examples.rdf4j

import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.rio.RDFFormat
import java.net.URL
import scala.util.Try

object CheckGuessMime extends App {

  val url = new URL("http://www.w3.org/2000/01/rdf-schema")

  val test = GuessMIME.guess(url)
  println(s"""GUESS FORMAT: "${test}"""")

}

object GuessMIME {

  def guess(url: URL) = {

    List(RDFFormat.TURTLE, RDFFormat.JSONLD, RDFFormat.RDFXML, RDFFormat.NTRIPLES, RDFFormat.N3)
      .map { format =>
        Try {
          val input = url.openStream()
          val test = Rio.parse(input, "", format)
          input.close()
          format.getDefaultMIMEType
        }
      }.toList
      .filterNot(_.isFailure)
      .map(_.get)
      .head

  }

}