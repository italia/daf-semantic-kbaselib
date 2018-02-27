package it.almawave.linkeddata.kb.parsers

import java.net.URL
import org.junit.Test
import org.junit.Assert
import it.almawave.linkeddata.kb.utils.URLHelper

class GuessRDFMIMETest {

  @Test
  def testing() {

    val urls = Array(
      ("TURTLE", new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/classifications-for-public-services/channel/channel.ttl")),
      ("TURTLE", new URL("http://www.w3.org/2000/01/rdf-schema")),
      ("RDF/XML", new URL("http://dati.gov.it/onto/clvapit#")))

    urls.foreach { url =>
      val follow_url = URLHelper.follow_redirect(url._2).get
      println(s"""test FORMAT: ${url._1} for ${url._2}""")
      val test = GuessRDFMIME.guess_format(follow_url).get
      println(s"""GUESSED FORMAT: ${test}""")
      Assert.assertEquals(url._1.toUpperCase(), test.getName.toUpperCase())
    }

  }

}

object MainGuessRDFMIME extends App {

  val url_src = new URL("http://dati.gov.it/onto/clvapit#")
  val follow_url = URLHelper.follow_redirect(url_src).get
  val ok = GuessRDFMIME.guess_format(follow_url).get
  println(ok)

}