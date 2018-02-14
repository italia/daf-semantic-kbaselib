package it.almawave.linkeddata.kb.standardization.NO

import it.almawave.linkeddata.kb.catalog.VocabularyBox
import com.typesafe.config.ConfigFactory
import it.almawave.linkeddata.kb.catalog.CatalogBox
import java.nio.file.Paths
import it.almawave.linkeddata.kb.catalog.SPARQL

object TestingStd extends App {

  val conf = ConfigFactory.parseFile(Paths.get("src/main/resources/conf/catalog.conf").normalize().toFile())
  val catalog = new CatalogBox(conf)
  catalog.start()

  val std = new StdQuery(catalog)

  val vocID = "AccommodationTypology" // CHECK Licenze

  val vbox: VocabularyBox = std.vocabularyWithDependency(vocID).get
  vbox.start()

  val ontologyID = vbox.extract_assetType()._1

  val LEVELS = 4

  val qq = query_h(
    vbox.id,
    List(("skos", "http://www.w3.org/2004/02/skos/core#")),
    "uri",
    List("skos:Concept"),
    List("vocabularyID"),
    List("code", "label"),
    "skos:broader",
    "",
    LEVELS)

    .split("\n").map(_.trim()).filterNot(_.equals("")).mkString("\n") // reduce noise in formatting. TODO: a proper parser for SPARQL

  println("SPARQL> " + qq)

  SPARQL(vbox.repo).query(qq)
    .foreach { item =>

      //      val json = JSONHelper.writeToString(item)
      println(item)

    }

  def query_h(
    vocabularyID:    String,
    prefixes:        Seq[(String, String)],
    sub:             String                = "uri",
    concepts:        Seq[String],
    bindings_all:    Seq[String],
    bindings_level:  Seq[String],
    parent_selector: String,
    cache:           String,
    MAX_LEVEL:       Int): String = {

    def q_bindings_all = bindings_all.map(b => s"?${b}").mkString(" ")
    def q_bindings_levels = (for (k <- 1 to MAX_LEVEL) yield bindings_level.map(b => s"?${b}_${k}").mkString(" ")).mkString(" ")

    def q_prefixes = prefixes.map(p => s"PREFIX ${p._1}: <${p._2}>").mkString("\n")

    def q_level(lvl: Int): String = {

      def q_optional = if (lvl > 0)
        s"""
          
        # using level ${lvl}
        ?${sub}_${lvl} a ${concepts(0)}  .
          
        OPTIONAL {
        
          ?${sub}_${lvl} ${parent_selector} ?parent_${lvl} . 
          
          FILTER(?parent_${lvl} = ?uri_${lvl - 1}) .
          
          ${q_level(lvl - 1)} 
          
        }
        """
      else ""

      def q_details() = s"""
      
        # this should be loaded from configuration or file!
        
        ?${sub}_${lvl} skos:notation ?code_${lvl} .
        
        ?${sub}_${lvl} skos:prefLabel ?label_${lvl} . BIND(LANG(?label_${lvl}) AS ?lang_${lvl}) . FILTER(?lang_${lvl} = 'it') .
        
      """

      def sub_query = s"""
        
        ${q_details()}
      
        ${q_optional} 
        
        BIND("${vocabularyID}" AS ?vocabularyID) .
        BIND(${lvl} AS ?level_${lvl}) . 
        
      """

      sub_query
    }

    s"""
      ${q_prefixes}
      
      SELECT ${q_bindings_all} ${q_bindings_levels} 
      
      WHERE {
        
        ${q_level(MAX_LEVEL)}
        
      }  
    """

  }

  vbox.stop()

}