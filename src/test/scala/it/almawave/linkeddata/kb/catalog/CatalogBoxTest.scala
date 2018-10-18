package it.almawave.linkeddata.kb.catalog

import com.typesafe.config.ConfigFactory
import java.nio.file.Paths
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.Assert
import java.net.URL
import java.io.File

class CatalogBoxTest {

  // TODO: create a config for testing purposes
  val conf = ConfigFactory.parseFile(Paths.get("src/test/resources/conf/catalog.conf").normalize().toFile())

  var catalog: CatalogBox = null

  @Before
  def start() {
    catalog = new CatalogBox(conf)
    catalog.start()
  }

  @After
  def stop() {
    catalog = null
  }

  @Test
  def test_loading() {
    Assert.assertTrue(catalog.ontologies.size > 0)
    Assert.assertTrue(catalog.ontologies_aligns.size > 0)
    Assert.assertTrue(catalog.vocabularies.size > 0)
  }
  @Test
  def test_skos_onto() {

    val onto_uri = "http://www.w3.org/TR/skos-reference/skos.rdf"
    val obox = OntologyBox.parse(new URL(onto_uri))
    Assert.assertEquals("skos", obox.id)

  }


//  @Test
  def test_ontology_default_context() {

    var obox = catalog.ontologies.find(_.id.equals("POI-AP_IT")).get
    Assert.assertEquals("http://dati.gov.it/onto/poiapit", obox.context)

    obox = catalog.ontologies.find(_.id.equals("TI-AP_IT")).get
    Assert.assertEquals("https://w3id.org/italia/onto/TI", obox.context)

  }

  @Test
  def test_ontologyBox_by_context() {

    val context = "http://dati.gov.it/onto/dcatapit"
    val obox = catalog.getOntologyByURI(context)

    println("FOUND: " + obox)

  }

  @Test
  def test_remote_ontologies() {

    Assert.assertTrue(catalog.externalOntologies.size > 0)

  }

  @Test
  def test_resolve() {

    println("\n\n\n\nTEST RESOLVE")
    //
    val voc_url = new File("src/test/resources/catalog-data/VocabolariControllati/licences/licences.ttl").toURI().toURL()
    val vbox = VocabularyBox.parse(voc_url)

    println("VBOX [not resolving ontologies]")
    println(vbox)

    val ontos = vbox.infer_ontologies()
    println("INFERRED: " + ontos)

    println(catalog.externalOntologies)

    val boxes_int = ontos.flatMap { onto_uri => catalog.ontologies.find(_.context.equals(onto_uri)) }
    println("INTERNAL: " + boxes_int)

    val boxes_ext = ontos.flatMap { onto_uri => catalog.externalOntologies.find(_.context.equals(onto_uri)) }
    println("EXTERNAL: " + boxes_ext)

    val vbox2 = vbox.federateWith(boxes_ext)
    println("VBOX [resolving ontologies]")
    println(vbox2)

    println("\n\n.........................................................")

    val vbox_ok = catalog.resolveVocabularyDependencies(vbox)
    println(vbox_ok)

  }

}