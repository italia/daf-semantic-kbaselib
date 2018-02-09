package it.almawave.linkeddata.kb.catalog

import it.almawave.linkeddata.kb.file.RDFFileRepository
import org.eclipse.rdf4j.repository.Repository
import java.net.URL
import org.slf4j.LoggerFactory
import it.almawave.linkeddata.kb.catalog.models.VocabularyMeta
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.memory.MemoryStore
import it.almawave.linkeddata.kb.catalog.models.URIWithLabel
import it.almawave.linkeddata.kb.catalog.models.ItemByLanguage
import it.almawave.linkeddata.kb.utils.URIHelper
import it.almawave.linkeddata.kb.catalog.models.Version
import it.almawave.linkeddata.kb.catalog.models.AssetType
import it.almawave.linkeddata.kb.parsers.VocabularyParser

object VocabularyBox {

  def parse(rdf_source: URL): VocabularyBox = {
    val parser = VocabularyParser(rdf_source)
    val meta = parser.parse_meta()
    new VocabularyBox(meta)
  }

}

class VocabularyBox(val meta: VocabularyMeta) extends RDFBox {

  override val id = meta.id
  override val context = meta.url.toString()

  override val repo: Repository = new RDFFileRepository(meta.source, context)

  // TODO: collecting sparql queries by configuration & convention

  override def toString() = s"""
    VocabularyBox [${id}, ${status}, ${triples} triples, ${context}]
  """.trim()

  def withImports() = {

    new VocabularyBoxWithImports(meta)

  }

}

// creating the internal RDFFIleRepository from all the rdf sources
class VocabularyBoxWithImports(meta: VocabularyMeta) extends VocabularyBox(meta) {

  // TODO: add dependencies in meta!
  val _dependencies = meta.dependencies.map { x => new URL(x) }.toList

  override val repo: Repository = new RDFFileRepository(meta.source :: _dependencies, meta.url.toString())

}

object MainVocabularyBoxWithImports extends App {

  val voc = VocabularyBox.parse(new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/ClassificazioneTerritorio/Istat-Classificazione-08-Territorio.ttl"))
  //  voc.start()
  //
  //  SPARQL(voc.repo).query("""
  //    SELECT DISTINCT ?concept
  //    WHERE {
  //      ?s a ?concept .
  //    }
  //  """)
  //    .map(_.toMap.getOrElse("concept", "").toString())
  //    .map(_.replaceAll("^.*[#/](.*?)$", "$1"))
  //    .foreach { item =>
  //      println(item)
  //    }
  //  voc.stop()
  //
  //  val t1 = voc.triples
  //  println("triples: " + t1)
  //  println(voc.concepts.mkString(" | "))
  //
  //  println("\n...............................\n")

  println("DEP " + voc.meta.dependencies)

  val extra = voc.withImports()

  println("DEP " + extra.meta.dependencies)

  System.exit(0)
  extra.start()

  SPARQL(extra.repo).query("""
    SELECT DISTINCT ?concept 
    WHERE {
      ?s a ?concept .
    }
  """)
    .map(_.toMap.getOrElse("concept", "").toString())
    .map(_.replaceAll("^.*[#/](.*?)$", "$1"))
    .foreach { item =>
      println(item)
    }
  extra.stop()

  println(extra.concepts.mkString(" | "))
  val t2 = extra.triples
  println("triples: " + t2)

}



