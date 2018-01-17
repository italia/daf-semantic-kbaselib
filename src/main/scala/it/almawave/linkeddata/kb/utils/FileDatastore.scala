package it.almawave.linkeddata.kb.utils

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

object FileDatastore {
  def apply(base: String) = {
    val base_path = Paths.get(base).toAbsolutePath().normalize()
    new FileDatastore(base_path)
  }
}

/*
 * draft for an helper class for handling files.
 * The idea is to encapsulate the logic for file access: read, save etc,
 * enabling later usage of HDFS and so on...
 */
class FileDatastore(val base_path: Path) {

  // TODO: see how to connect this to HDFS or similar

  val logger = LoggerFactory.getLogger(this.getClass)

  //  @deprecated
  //  def cache(name: String, doc_path: String) {
  //
  //    val base_path: Path = try {
  //
  //      val uri = URI.create(doc_path).normalize()
  //      val url = uri.toURL()
  //
  //      val path = Paths.get(base, name, uri.getPath.substring(uri.getPath.lastIndexOf("/")))
  //
  //      val dir = path.toFile().getParentFile
  //      if (!dir.exists()) dir.mkdirs()
  //
  //      if (!path.toFile().exists()) {
  //        Files.copy(url.openStream(), path)
  //      } else {
  //        logger.debug(s"KB> ${name} already cached in ${path}!")
  //      }
  //
  //      path
  //
  //    } catch {
  //      case ex: Exception =>
  //        ex.printStackTrace()
  //
  //        var base_path = base
  //
  //        if (base.startsWith("file:/"))
  //          base_path = base.replaceFirst("^.*(/.*)$", "$1")
  //
  //        Paths.get(base_path).toAbsolutePath().normalize()
  //
  //    }
  //  }

  /**
   * gets the file list
   */
  def listFile(depth: Int, extension: String*): Stream[URI] = {

    Files.walk(base_path, depth).iterator().toStream
      .filter(_.toFile().isFile())
      .filter(_.toString().matches(s".*\\.(${extension.mkString("|")})"))
      .map(_.toUri().normalize())

  }

  /**
   * gets the dir list
   */
  def listDir(contains: String*): Stream[URI] = {

    Files.walk(base_path).iterator().toStream
      .filter(_.toFile().isDirectory())
      .filter(p => contains.exists(e => p.toString().matches(s".*${e}.*")))
      .map(_.toUri().normalize())

  }

}


