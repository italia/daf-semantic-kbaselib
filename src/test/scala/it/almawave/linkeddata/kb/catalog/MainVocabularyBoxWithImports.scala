package it.almawave.linkeddata.kb.catalog

import java.net.URL

// just for simple testing, it will be removed!
object MainVocabularyBoxWithImports extends App {

  val voc = VocabularyBox.parse(new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/ClassificazioneTerritorio/Istat-Classificazione-08-Territorio.ttl"))
  voc.start()

  println("DEP " + voc.meta.dependencies)

  //  TODO: REVIEW OR DROP THIS
  //
  //
  //  val extra = voc.withImports()
  //
  //  println("DEP " + extra.meta.dependencies)
  //
  //  System.exit(0)
  //  extra.start()
  //
  //  SPARQL(extra.repo).query("""
  //    SELECT DISTINCT ?concept
  //    WHERE {
  //      ?s a ?concept .
  //    }
  //  """)
  //    .map(_.toMap.getOrElse("concept", "").toString())
  //    .map(_.replaceAll("^.*[#/](.*?)$", "$1"))
  //    .foreach { item =>
  //      println(item)
  //    }
  //  extra.stop()
  //
  //  println(extra.concepts.mkString(" | "))
  //  val t2 = extra.triples
  //  println("triples: " + t2)

  voc.stop()

}



