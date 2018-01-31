package it.almawave.linkeddata.kb.utils

object URIHelper {
  def extractLabelFromURI(uri: String) = uri.replaceAll("^.*\\/.*[#/](.*?)$", "$1")
}

object TestingURIHelper extends App {

  val uri = "http://publications.europa.eu/resource/authority/data-theme/TECH"
  val label = URIHelper.extractLabelFromURI(uri)

  println("LABEL: " + label)

}