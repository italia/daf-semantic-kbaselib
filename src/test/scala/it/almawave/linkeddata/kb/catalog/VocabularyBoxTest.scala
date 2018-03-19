package it.almawave.linkeddata.kb.catalog

import java.io.File
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.Assert

class VocabularyBoxTest {

  val voc_url = new File("src/test/resources/catalog-data/VocabolariControllati/licences/licences.ttl").toURI().toURL()

  var vbox: VocabularyBox = null

  @Before
  def start() {
    vbox = VocabularyBox.parse(voc_url)
    vbox.start()
  }

  @After
  def stop() {
    vbox.stop()
    vbox = null
  }

  @Test
  def test_id() {
    Assert.assertEquals("licences", vbox.id)
  }

  @Test
  def test_has_triples() {
    val triples = vbox.triples
    Assert.assertTrue(triples > 0)
  }

  @Test
  def test_context() {
    val context = vbox.context
    Assert.assertTrue(context != null && context.size > 0)
    println(s"context for ${vbox.id}: ${context}")
    Assert.assertEquals("http://w3id.org/italia/controlled-vocabulary/licences", context)
  }

  @Test
  def has_concepts() {
    val concepts = vbox.concepts.map(_._1).toList
    println(s"concepts in ${vbox.id}:\n\t" + concepts.mkString("\n\t"))
    Assert.assertTrue(concepts.size > 0)
    Assert.assertTrue(concepts.contains("Dataset")) // test if the vocabulary has been correctly annotated with DCAT
    Assert.assertTrue(concepts.contains("Class"))
  }

  @Test
  def test_dependencies() {

    val ontos = vbox.infer_ontologies()
    Assert.assertTrue(ontos.toList.size > 0)
    Assert.assertEquals("http://www.w3.org/2004/02/skos/core", ontos(0))

  }

}