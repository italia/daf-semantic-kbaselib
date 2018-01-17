package it.almawave.linkeddata.kb.utils

import java.io.OutputStream
import java.net.URI

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import org.eclipse.rdf4j.repository.RepositoryResult
import org.eclipse.rdf4j.query.TupleQueryResult
import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.sail.memory.model.MemLiteral
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.sail.memory.model.MemIRI
import org.eclipse.rdf4j.model.impl.SimpleIRI
import org.eclipse.rdf4j.sail.memory.model.MemBNode
import org.eclipse.rdf4j.model.ValueFactory
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.query.Binding

import java.util.{ LinkedHashSet, LinkedHashMap, Map => JMap }
import org.eclipse.rdf4j.sail.memory.model._

import java.lang.{ Boolean => JBoolean }
import org.eclipse.rdf4j.model.impl.BooleanLiteral
import org.eclipse.rdf4j.model.impl.CalendarLiteral
import org.eclipse.rdf4j.model.impl.DecimalLiteral
import org.eclipse.rdf4j.model.impl.NumericLiteral
import org.eclipse.rdf4j.model.impl.IntegerLiteral
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.impl.SimpleLiteral
import org.eclipse.rdf4j.model.impl.SimpleBNode

object RDF4JAdapters {

  implicit class StringContextAdapter(context: String) {
    def toIRI(implicit vf: ValueFactory = SimpleValueFactory.getInstance): IRI = {
      vf.createIRI(context)
    }
  }

  implicit class StringContextListAdapter(contexts: Seq[String]) {
    def toIRIList(implicit vf: ValueFactory = SimpleValueFactory.getInstance): Seq[IRI] = {
      contexts.map { cx => vf.createIRI(cx) }
    }
  }

  def prettyPrint(doc: Model, out: OutputStream, format: RDFFormat = RDFFormat.TURTLE) = ???

  implicit class RepositoryResultIterator[T](result: RepositoryResult[T]) extends Iterator[T] {
    def hasNext: Boolean = result.hasNext()
    def next(): T = result.next()
  }

  implicit class TupleResultIterator(result: TupleQueryResult) extends Iterator[BindingSet] {
    def hasNext: Boolean = result.hasNext()
    def next(): BindingSet = result.next()
  }

  implicit class BindingSetMapAdapter(bs: BindingSet) {

    // TODO: add a test for checking the correct conversion

    def toMap(): Map[String, Object] = {

      // CHECK: key order
      val names = bs.getBindingNames

      names.map { n =>

        val binding: Binding = bs.getBinding(n)

        // CHECK: default values extraction?

        val name = binding.getName

        val value: Object = binding.getValue

        //        match {
        //
        //          //          case bool: BooleanMemLiteral      => bool.booleanValue().asInstanceOf[JBoolean]
        //          //          case bool: BooleanLiteral         => bool.booleanValue().asInstanceOf[JBoolean]
        //          //
        //          //          case calendar: CalendarMemLiteral => calendar.calendarValue()
        //          //          case calendar: CalendarLiteral    => calendar.calendarValue()
        //          //
        //          //          case decimal: DecimalMemLiteral   => decimal.decimalValue()
        //          //          case decimal: DecimalLiteral      => decimal.decimalValue()
        //
        //          //          case integer: IntegerMemLiteral => integer.integerValue()
        //          //          case integer: IntegerLiteral    => integer.integerValue()
        //          //
        //          //          case numeric: NumericMemLiteral   => numeric.decimalValue()
        //          //          case numeric: NumericLiteral      => numeric.decimalValue()
        //
        //          case literal: MemLiteral    => literal.stringValue()
        //          case literal: SimpleLiteral => literal.stringValue()
        //          case literal: Literal       => literal.stringValue()
        //
        //          case iri: MemIRI            => new URI(iri.stringValue())
        //          case iri: SimpleIRI         => new URI(iri.stringValue())
        //
        //          case bnode: MemBNode        => bnode.stringValue() // CHECK: bnodes serialization
        //          case bnode: SimpleBNode     => bnode.stringValue() // CHECK: bnodes serialization
        //
        //          case other                  => other
        //
        //        }

        (name, value)

      }.toMap
    }

  }

}