package it.almawave.linkeddata.kb.catalog

import java.io.File
import org.junit.Test
import org.junit.Assert
import org.junit.Before
import org.junit.After
import java.net.URL

/**
 * A minimal test for OntologyBox
 *
 * TODO: add more coverage, and detailed tests
 */
class OntologyBoxTest {

  val onto_url = new File("src/test/resources/catalog-data/Ontologie/CLV/latest/CLV-AP_IT.ttl").toURI().toURL()
  var obox: OntologyBox = null

  @Before
  def start() {
    obox = OntologyBox.parse(onto_url)
    obox.start()
  }

  @After
  def stop() {
    obox.stop()
    obox = null
  }

  @Test
  def test_id() {
    Assert.assertEquals("CLV-AP_IT", obox.id)
  }

  @Test
  def test_has_triples() {
    val triples = obox.triples
    Assert.assertTrue(triples > 0)
  }

  @Test
  def test_context() {
    val context = obox.context
    Assert.assertTrue(context != null && context.size > 0)
    println(s"context for ${obox.id}: ${context}")
    Assert.assertEquals("https://w3id.org/italia/onto/CLV", context)
    // TODO: Assert.assertEquals("https://w3id.org/italia/onto/CLV", context)
  }

  @Test
  def has_concepts() {
    val concepts = obox.concepts.map(_._1).toList
    println(obox)
    println(s"concepts in ${obox.id}:\n\t" + concepts.mkString("\n\t"))
    Assert.assertTrue(concepts.size > 0)
    Assert.assertTrue(concepts.contains("Ontology"))
    Assert.assertTrue(concepts.contains("Class"))
  }

}