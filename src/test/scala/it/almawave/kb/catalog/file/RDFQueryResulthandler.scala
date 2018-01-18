package it.almawave.kb.catalog.file

import org.eclipse.rdf4j.query.TupleQueryResultHandler
import scala.collection.mutable.ListBuffer
import org.eclipse.rdf4j.query.BindingSet
import scala.collection.SortedSet
import scala.collection.mutable.SetBuilder

class RDFQueryResultHandler extends TupleQueryResultHandler {

  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._
  import java.util.{ List => JList }

  private var names: List[String] = List.empty

  private val _results = new ListBuffer[BindingSet]

  def handleBoolean(value: Boolean) {}
  def handleLinks(links: JList[String]) {}

  def handleSolution(bs: BindingSet) {
    _results += bs
  }

  def startQueryResult(bindingNames: JList[String]) {
    names = bindingNames.toList
  }
  
  def endQueryResult() {}

  def toStream = _results.toStream

}