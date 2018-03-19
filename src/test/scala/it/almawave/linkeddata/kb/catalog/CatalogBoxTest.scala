package it.almawave.linkeddata.kb.catalog

import com.typesafe.config.ConfigFactory
import java.nio.file.Paths
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.Assert
import java.net.URL

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
    Assert.assertTrue(catalog.vocabularies.size > 0)
  }

  @Test
  def test_dependencies() {

    val onto_uri = "http://www.w3.org/TR/skos-reference/skos.rdf"
    val obox = OntologyBox.parse(new URL(onto_uri))

    Assert.assertEquals("skos", obox.id)

  }

  @Test
  def ontology_default_context() {

    var obox = catalog.ontologies.find(_.id.equals("POI-AP_IT")).get
    Assert.assertEquals("http://dati.gov.it/onto/poiapit", obox.context)

    obox = catalog.ontologies.find(_.id.equals("TI-AP_IT")).get
    Assert.assertEquals("https://w3id.org/italia/onto/TI", obox.context)

  }

  @Test
  def ontologyBox_by_context() {

    val context = "http://dati.gov.it/onto/dcatapit"
    val obox = catalog.getOntologyByURI(context)

    println("FOUND: " + obox)

  }

}