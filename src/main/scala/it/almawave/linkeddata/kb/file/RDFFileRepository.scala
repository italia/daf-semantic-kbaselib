package it.almawave.linkeddata.kb.file

import java.net.URL

import org.eclipse.rdf4j.repository.sail.SailRepository

class RDFFileRepository(url: URL)
  extends SailRepository(new RDFFileSail(url))
