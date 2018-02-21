package it.almawave.linkeddata.kb.parsers

import java.net.URL
import it.almawave.linkeddata.kb.catalog.VocabularyBox
import java.net.HttpURLConnection
import it.almawave.linkeddata.kb.utils.URLHelper

// TODO: review or drop this
object TESTRDF4JXMLParser extends App {

  // GENRAL CONFIG
  HttpURLConnection.setFollowRedirects(true)

  val urls = List(
    //    new File("C:/Users/Al.Serafini/repos/DAF/daf-ontologie-vocabolari-controllati/Ontologie/IndirizziLuoghi/latest/CLV-AP_IT.rdf").toURI().toURL()
    //    new URL("http://dati.gov.it/onto/clvapit")
    //    new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/ClassificazioneTerritorio/Istat-Classificazione-08-Territorio.ttl")
    //    new URL("file:///C:/Users/Al.Serafini/repos/DAF/daf-ontologie-vocabolari-controllati/VocabolariControllati/Licenze/Licenze.ttl")
    new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/Licenze/Licenze.ttl"))

  urls.foreach { url =>

    println("\n\n_________________________\n\nURL> " + url)

    val url_redirected = URLHelper.follow_redirect(url)

    val box: VocabularyBox = VocabularyBox.parse(url_redirected.get)
    box.start()

    println("BOX: " + box)
    println("META: " + box.meta)

    //    val json = JSONHelper.writeToString(box.meta)
    //    println("JSON: " + json)

    println("BOX: " + box.triples)

    //    println("BOX with imports: " + box.withImports().triples)

    box.stop()

  }

}


//    // read body
//    conn.connect()
//    val input = conn.getInputStream
//    val src = Source.fromInputStream(input)
//    val txt = src.getLines().mkString
//    println(txt)
//    src.close()
//    input.close()
//    conn.disconnect()

