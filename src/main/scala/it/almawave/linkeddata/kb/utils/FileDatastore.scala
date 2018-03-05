package it.almawave.linkeddata.kb.utils

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import java.io.File
import scala.annotation.tailrec

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

  /**
   * gets the file list
   */
  def listFile(depth: Int, extension: String*): Stream[URI] = {

    Files.walk(base_path, depth).iterator().toStream
      .filter(_.toFile().isFile())
      .filter(_.toString().matches(s".*\\.(${extension.mkString("|")})"))
      .map(_.toUri().normalize())

  }

  def listFilesIn(item: File): Seq[File] = {

    item match {

      case file if (file.isFile()) => Stream(file)

      case dir if (dir.isDirectory()) =>
        item #:: dir.listFiles().toStream.flatMap(el => listFilesIn(el)).toStream

    }

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


