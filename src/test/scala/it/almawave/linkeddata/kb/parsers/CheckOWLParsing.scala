package it.almawave.linkeddata.kb.parsers

import java.net.URL
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.rio.RDFFormat

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

object CheckOWLParsing extends App {

  val url = new URL("https://www.dati.gov.it/onto/DCAT-AP_IT.owl")
  val input = url.openStream()

  val model = Rio.parse(input, "", RDFFormat.RDFXML).iterator().toList

  model.foreach { st =>
    println(st)
  }

  input.close()

}