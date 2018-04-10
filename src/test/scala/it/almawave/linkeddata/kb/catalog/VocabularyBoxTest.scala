package it.almawave.linkeddata.kb.catalog

import java.io.File
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.Assert
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.URL
import org.junit.runners.Parameterized.Parameters

object MainVOC extends App {

  val voc_url = new File("src/test/resources/catalog-data/VocabolariControllati/licences/licences.ttl").toURI().toURL()

  var vbox = VocabularyBox.parse(voc_url)
  println(vbox)

  SPARQL(vbox.repo).query("""
    SELECT *
    WHERE {
      ?s ?p ?o
    }  
  """)
    .foreach(println)

}

class VocabularyBoxTest() {

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
  def test_infer_ontologies() {
    val ontos = vbox.infer_ontologies()
    Assert.assertTrue(ontos.toList.size > 0)
    Assert.assertEquals("http://www.w3.org/2004/02/skos/core#", ontos(0))
    // TODO: local resolution of SKOS
  }

  @Test
  def test_asset_type() {
    val ontoID = vbox.extract_assetType()._1
    Assert.assertEquals("SKOS", ontoID)
  }

  @Test
  def test_with_dependencies() {

    val triples_before = vbox.triples
    println("BEFORE: " + triples_before)

    val skos_onto = OntologyBox.parse(new URL("https://www.w3.org/TR/skos-reference/skos.rdf"))
    val vboxF = vbox.federateWith(List(skos_onto))

    val triples_after = vboxF.triples
    println("AFTER: " + triples_after)

    Assert.assertTrue(triples_after > triples_before)
    Assert.assertEquals(504, triples_after - triples_before)

  }

}


//REVIEW

//@RunWith(value = classOf[Parameterized])
//class VocabularyBoxTest(voc_url: VocabularyTest) {
//...
//...
//object VocabularyBoxTest {
//
//  // NOTE: Must return collection of Array[AnyRef] (NOT Array[Any]).
//  @Parameters def parameters: java.util.Collection[Array[VocabularyTest]] = {
//
//    val params = Array(
//
//      VocabularyTest(
//        "licences",
//        new File("src/test/resources/catalog-data/VocabolariControllati/licences/licences.ttl").toURI().toURL(),
//        "http://www.w3.org/2004/02/skos/core"))
//
//    val list = new java.util.ArrayList[Array[VocabularyTest]]()
//    list.add(params)
//
//    list
//  }
//}
//
//case class VocabularyTest(
//  id:      String,
//  src:     URL,
//  context: String)

