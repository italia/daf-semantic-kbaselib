package it.almawave.linkeddata.kb.parsers

import java.net.URL
import java.nio.file.Paths

import it.almawave.linkeddata.kb.catalog.OntologyBox
import it.almawave.linkeddata.kb.utils.JSONHelper

/*
 * ontology metadata extraction example
 *
 * TODO: provide a proper JUnit test
 */
object MainOntologyBoxParser extends App {

  //val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/Ontologie/Organizzazioni/latest/COV-AP_IT.ttl")

  val fileRdf = Paths.get("c:/Users/a.mauro/IdeaProjects/katalod/ontologie-vocabolari-controllati/Ontologie/ADMS/latest/ADMS-AP_IT.rdf").toString
  val fileQuerySparql = Paths.get("C:/Users/a.mauro/IdeaProjects/katalod/conf/query/skos/hierarchyOneLevel.sparql").toFile
  val skos_url = new URL(s"file:///$fileRdf")


  println(s"\n\nextracting informations from\n${skos_url}\n...")

  val box = OntologyBox.parse(skos_url)
  box.start()
  println(box)

  val json = JSONHelper.writeToString(box.meta)
  println(json)

  box.stop()

}