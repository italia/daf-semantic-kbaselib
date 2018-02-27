package it.almawave.linkeddata.kb.parsers

import java.net.URL
import org.junit.Test
import org.junit.Assert

class GuessRDFMIMETest {

  @Test
  def testing() {

    val urls = Array(
      ("TURTLE", new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/classifications-for-public-services/channel/channel.ttl")),
      ("TURTLE", new URL("http://www.w3.org/2000/01/rdf-schema")),
      ("RDF", new URL("http://dati.gov.it/onto/clvapit#")))

    urls.foreach { url =>
      println(s"""test FORMAT: ${url._1} for ${url._2}""")
      val test = GuessRDFMIME.guess_format(url._2).get
      println(s"""GUESSED FORMAT: ${test}""")
      Assert.assertEquals(url._1.toUpperCase(), test.getName.toUpperCase())
    }

  }

}

object MainGuessRDFMIME extends App {

  //  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/MappingTema-Sottotema/MappingDataThemeEurovoc.rdf")

  val url = new URL("http://dati.gov.it/onto/clvapit#")
  //    val url = new File("D:/CLV-AP_IT_301").toURI().toURL()
  //  val url = new File("D:/CLV-AP_IT_protege").toURI().toURL()
  //      val url = new File("D:/CLV-AP_IT").toURI().toURL()

  val ok = GuessRDFMIME.guess_format(url)
  println(ok)

}