package it.almawave.linkeddata.kb.standardization

import java.nio.file.Paths
import it.almawave.linkeddata.kb.catalog.CatalogBox
import com.typesafe.config.ConfigFactory
import it.almawave.linkeddata.kb.catalog.SPARQL
import it.almawave.linkeddata.kb.utils.JSONHelper
import scala.util.Try
import org.slf4j.LoggerFactory

object TestingStandardizationHelper extends App {

  private val logger = LoggerFactory.getLogger(this.getClass)

  val conf = ConfigFactory.parseFile(Paths.get("src/main/resources/conf/catalog.conf").normalize().toFile())
  val catalog = new CatalogBox(conf)
  catalog.start()

  val std = new StandardizationProcess(catalog)

  //   ALL
  std.vocabulariesWithDependencies().foreach { vbox =>

    // DEBUG single
    //    val vocID = "Licenze" //AccommodationTypology"
    //    val vbox = std.vocabularyWithDependency(vocID).get

    Try {

      vbox.start()
      logger.info(s"\n\n#### Vocabulary: ${vbox} ####")

      // DEBUG
      val cells = std.standardize_data(vbox) // TODO: introduce a model!
      //        .foreach { item =>
      //          logger.info("\nITEM:\n\t" + item.map(e => (e.name, e.value)).mkString("\n\t"))
      //        }

      logger.debug(JSONHelper.writeToString(cells.toList))

      val MAX_LEVELS = std.max_levels(vbox).get
      logger.info("MAX_LEVELS: " + MAX_LEVELS)

      vbox.stop()

      logger.info(s"#############################\n\n")
    }

  } // ALL

  catalog.stop()
}