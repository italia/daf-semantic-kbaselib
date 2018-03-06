package it.almawave.linkeddata.kb.catalog

import java.io.File

/*
 *  TODO: parametric extraction of RDF file list
 */
class RDFFilesStore(root: File) {

  def vocabularies() = files_list(root)
    .filter(_.getName.endsWith("ttl"))
    .filter(_.toString().contains("VocabolariControllati"))

  def ontologies() = files_list(root)
    .filter(_.getName.endsWith("ttl"))
    .filter(_.toString().contains("Ontologie"))
    .filter(_.toString().contains("latest"))

  def files_list_by_extension(element: File, ext: String): Seq[File] =
    files_list(element).filter(f => f.getName.endsWith(ext))

  def files_list(element: File): Seq[File] = {

    element match {

      case file if (file.isFile()) =>
        Stream(file)
      case dir if (dir.isDirectory()) =>
        Stream(dir) ++ dir.listFiles().toStream.flatMap { f => files_list(f) }

    }

  }

}
