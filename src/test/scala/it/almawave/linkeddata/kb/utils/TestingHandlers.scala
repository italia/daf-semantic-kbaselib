package it.almawave.linkeddata.kb.utils

import org.slf4j.LoggerFactory
import java.io.PrintWriter
import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import scala.collection.mutable.ListBuffer
import org.eclipse.rdf4j.model.Statement

/*
 * this is a simple class for verifying the TryHandlers helper class
 */
object TestingHandlers extends App {

  import it.almawave.linkeddata.kb.utils.TryHandlers._

  val logger = LoggerFactory.getLogger(this.getClass)

  using(new PrintWriter("sample.txt")) { out =>
    out.println("hello world!")
  }

  logErrors(logger) {
    logger =>
      println("vediamo...")
  }

  val repo = new MemoryStore()
  repo.initialize()

  using(repo.getConnection) {

    conn =>
      // IDEA...
      doInTransaction(conn) {
        conn =>
          val vf = SimpleValueFactory.getInstance
          conn.addStatement(vf.createIRI("http://sub_01"), vf.createIRI("http://prp_01"), vf.createLiteral("obj_01"))
      }

  }

  // IDEA...
  val elements = using(repo.getConnection) { conn =>
    val list = new ListBuffer[Statement]
    val results = conn.getStatements(null, null, null, false)
    while (results.hasNext()) {
      list += results.next()
    }
    list.toStream
  }
  println(elements.mkString("|"))

  repo.shutDown()

}