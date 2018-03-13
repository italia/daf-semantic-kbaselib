package it.almawave.linkeddata.kb.catalog

import org.junit.Before
import org.junit.After
import org.junit.Test
import java.net.URL
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import it.almawave.linkeddata.kb.repo.RDFRepository
import it.almawave.linkeddata.kb.repo.RDFRepositoryBase
import org.junit.Assert
import java.nio.file.Paths

class TestingRDFCatalogmanager {

  val data_dir = "src/test/resources/catalog-data"
  val mock: RDFRepositoryBase = RDFRepository.memory()

  @Before()
  def before() {

    org.junit.Assume.assumeTrue(mock.isAlive().get)

    mock.start()
    mock.store.clear()
  }

  @After()
  def after() {
    mock.stop()
  }

  @Test
  def add_ontology() {

    val onto_id = "CLV-AP_IT"
    val onto_path = Paths.get(s"${data_dir}/Ontologie/CLV/latest/CLV-AP_IT.ttl").normalize()
    val onto_prefix = "clvapit"
    val onto_context = ontology_uri(onto_id)
    val onto_mime = "text/turtle"
    val onto_base = onto_context

    val size_before = mock.store.size().get

    mock.catalog.addOntology(onto_path.toUri().toURL(), onto_mime, onto_id, onto_prefix, onto_base, onto_context)

    val size_after = mock.store.size().get

    Assert.assertTrue(size_after > size_before)

    val prefixesMap = mock.prefixes.list().get
    Assert.assertTrue(prefixesMap.size > 0)
    Assert.assertTrue(prefixesMap.contains(onto_prefix))

    val contexts = mock.store.contexts().get
    Assert.assertTrue(contexts.contains(onto_context))

  }

  @Test
  def remove_ontology() {

    val onto_id = "CLV-AP_IT"
    val onto_context = ontology_uri(onto_id)
    add_ontology() // TODO: refactorization here (avoiding dependent test)!!
    mock.catalog.removeOntologyByURI(onto_context)

    Assert.assertEquals(0, mock.store.size(onto_context).get)

  }

  @Test
  def add_vocabulary() {

    val voc_id = "licences"
    val voc_path = Paths.get(s"${data_dir}/VocabolariControllati/licences/licences.ttl").normalize()
    val voc_mime = "text/turtle"

    // choose a context!
    val voc_context = vocabulary_uri(voc_id)
    val voc_base = voc_context

    mock.store.clear()

    val size_before = mock.store.size(voc_context).get
    println("SIZE BEFORE?? " + size_before)
    Assert.assertTrue(size_before == 0)

    mock.catalog.addVocabulary(voc_path.toUri().toURL(), voc_mime, voc_id, voc_base, voc_context)

    val size_after = mock.store.size(voc_context).get
    println("SIZE AFTER?? " + size_after)

    Assert.assertTrue(size_after > size_before)

    val contexts = mock.store.contexts().get
    Assert.assertTrue(contexts.contains(voc_context))

  }

  @Test
  def remove_vocabulary() {

    val voc_id = "licences"
    val voc_context = vocabulary_uri(voc_id)

    add_vocabulary() // TODO: refactorization!!
    mock.catalog.removeVocabularyByURI(voc_context)

    Assert.assertEquals(0, mock.store.size(voc_context).get)

  }

  def ontology_uri(id: String) = s"test://dati.gov.it/ontologies/${id}/"
  def vocabulary_uri(id: String) = s"test://dati.gov.it/vocabularies/${id}/"

}
