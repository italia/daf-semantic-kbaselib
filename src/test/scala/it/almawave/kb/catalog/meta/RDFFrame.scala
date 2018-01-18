package it.almawave.kb.catalog.meta

/**
 * TODO
 *
 * NOTE: this is currently only an experiment
 */
object RDFFrameQueries {

  def propertiesList(concept: String) = s"""
      SELECT DISTINCT ?property 
      WHERE { 
        <${concept}> a owl:Class ; 
        ?property [] .
        ?other ?inverse <${concept}> . 
      }
    """

  def isDomainOf(concept: String) = s"""
      SELECT DISTINCT ?property ?foreign 
      WHERE { 
        <${concept}> a owl:Class .
        ?property rdfs:domain <${concept}> . 
      }"""

  def isRangeOf(concept: String) = s"""
      SELECT DISTINCT ?property ?foreign 
      WHERE { 
        <${concept}> a owl:Class .
        ?property rdfs:range <${concept}> . 
      }
    """

  def inversesList(concept: String) = s"""
      SELECT DISTINCT ?property ?foreign 
      WHERE { 
        <${concept}> a owl:Class .
        ?other a owl:Class .
        ?other ?property <${concept}> . 
      }
    """

}
case class RDFFrame()

