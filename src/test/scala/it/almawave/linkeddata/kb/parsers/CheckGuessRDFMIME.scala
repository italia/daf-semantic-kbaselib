package it.almawave.linkeddata.kb.parsers

import java.net.URL

object CheckGuessRDFMIME extends App {

  val url = new URL("http://www.w3.org/2000/01/rdf-schema")
  val test = GuessRDFMIME.guess(url)
  println(s"""GUESS FORMAT: "${test}"""")

}