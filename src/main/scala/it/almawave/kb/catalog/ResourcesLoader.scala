package it.almawave.kb.catalog

import com.typesafe.config.ConfigFactory
import java.net.URL
import java.nio.file.Paths
import it.almawave.kb.catalog.meta.OntologyMetadataExtractor
import it.almawave.kb.catalog.models.OntologyMeta
import it.almawave.kb.catalog.models.VocabularyMeta
import it.almawave.kb.catalog.meta.VocabularyMetadataExtractor
import com.typesafe.config.Config

object ResourcesLoader {

  def apply(config_path: String) =
    new ResourcesLoader(ConfigFactory.parseFileAnySyntax(Paths.get(config_path).normalize().toFile()))

  //  def apply(config_path: String) = new ResourcesLoader(ConfigFactory.parseResources(config_path))

  def create(conf: Config) = {
    new ResourcesLoader(conf)
  }

}

class ResourcesLoader(configuration: Config) {

  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

  val conf = configuration.resolve()

  def cacheFor(url: URL): URL = {

    //local: "./target/EXPORT/git"
    //remote: "https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master"

    val modified_url = url.toString().replaceAll(conf.getString("remote"), conf.getString("local"))

    val path = Paths.get(modified_url).normalize().toUri()

    path.toURL()
  }

  def fetchOntologies(useCache: Boolean = true): Seq[OntologyMeta] = {

    val onto_conf = conf.getConfig("ontologies")

    val local = conf.getString("local").trim()
    val remote = conf.getString("remote").trim()
    val onto_base = onto_conf.getString("base").trim()

    onto_conf
      .getConfigList("data")
      .toStream
      .map { item =>

        val onto_path = item.getValue("path").unwrapped()
        // TODO: use local cache!
        val onto_source: URL = new URL(s"${remote}${onto_path}")
        val onto_cache: URL = Paths.get(s"${local}${onto_path}").normalize().toUri().toURL()

        val data_url = if (useCache) onto_source else onto_source

        OntologyMetadataExtractor(data_url).meta

      }

  }

  def fetchVocabularies(useCache: Boolean = true): Seq[VocabularyMeta] = {

    val onto_conf = conf.getConfig("vocabularies")

    val local = conf.getString("local").trim()
    val remote = conf.getString("remote").trim()
    val onto_base = onto_conf.getString("base").trim()

    onto_conf
      .getConfigList("data")
      .toStream
      .map { item =>
        val onto_path = item.getValue("path").unwrapped()
        // TODO: use local cache!
        val voc_source: URL = new URL(s"${remote}${onto_path}")
        val voc_cache: URL = Paths.get(s"${local}${onto_path}").normalize().toUri().toURL()

        val data_url = if (useCache) voc_source else voc_source

        VocabularyMetadataExtractor(data_url).meta

      }

  }

}

