package it.almawave.kb.catalog.file

import java.net.URL
import it.almawave.kb.catalog.file.RDFFileSail

class OntologySail(onto: OntologyInfo) extends RDFFileSail(onto.source, onto.contexts: _*) {

}

case class OntologyInfo(
  ontologyID: String,
  prefix: String,
  namespace: String,
  source: URL,
  contexts: String*)