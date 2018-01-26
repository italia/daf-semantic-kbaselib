package it.almawave.linkeddata.kb.file

import java.net.URL

import org.eclipse.rdf4j.repository.sail.SailRepository

class RDFFileRepository(urls: Seq[URL], contexts: String*) extends SailRepository(new RDFFileSail(urls)) {

  //  class RDFFileSail(urls: Seq[URL], contexts: String*) extends MemoryStore {
  //
  def this(url: URL, contexts: String*) = this(List(url), contexts: _*)

}
