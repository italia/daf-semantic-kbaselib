package it.almawave.linkeddata.kb.parsers

import java.net.URL

import com.sun.xml.internal.bind.v2.TODO
import org.eclipse.rdf4j.repository.Repository
import it.almawave.linkeddata.kb.file.RDFFileRepository
import org.slf4j.LoggerFactory
import it.almawave.linkeddata.kb.catalog.SPARQL
import it.almawave.linkeddata.kb.catalog.models._
import it.almawave.linkeddata.kb.utils.URIHelper
import it.almawave.linkeddata.kb.utils.DateHelper
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.sail.federation.Federation

object OntologyExtendedParser {

  def apply(rdf_source1: URL, rdf_source2: URL) = {
    val repo1: Repository = new RDFFileRepository(rdf_source1)
    val repo2: Repository = new RDFFileRepository(rdf_source2)


    val federation = new Federation
    if(rdf_source1 != null) federation.addMember(repo1)
    federation.addMember(repo2)

    // TODO: extract prefixes from OntologyBox

    val repo = new SailRepository(federation)


    // TODO da verificare se va bene il principale "rdf source"
    new OntologyExtendedParser(repo, rdf_source2)
  }

}

// TODO: move into parser package
class OntologyExtendedParser(val repo: Repository, rdf_source: URL) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  // the repository should be active! ASK: how can we avoid this check?
  { if (!repo.isInitialized()) repo.initialize() }

  logger.debug(s"parsing metadata for ${rdf_source}")

  val id: String = this.parse_id() //rdf_source.getPath.replaceAll(".*/(.*?)\\.[a-z]+$", "$1").trim()

  var _hasContributor: String = ""
  var _hasFormalityLevel: String = ""
  var _hasOntologyLanguage: String = ""
  this.parse_hasGroup()

  /*
   * NOTE:
   * 	- regex on SKOS source should extract core instead of /2004/02/skos/core
   * 	- SKOS and other similar external ontologies should be loaded, adding an external configuration
   * 		in order to handle the lack of standard configurations adopted in the network
   */
  val prefix: String = id.replaceAll("_", "").replaceAll("-", "").toLowerCase().trim()

  def parse_meta(url: String): OntologyExtendedMeta = {

    //    TODO: validation: add a SPARQL query to verify if the source is actually an Ontology
    //    this.is_ontology()

    val namespace = this.parse_namespace()
    val onto_url = this.parse_onto_url()
    val concepts = this.parse_concepts()
    val imports = this.parse_imports()
    val titles = this.parse_titles()
    val descriptions = this.parse_descriptions()
    val creators = this.parse_creators()
    val versions = this.parse_versions()
    val langs = this.parse_langs()

    val publishedBy = this.parse_publishedBy()
    val owners = this.parse_owner()
    val licenses = this.parse_licenses()

    val creationDate = this.parse_creationDate()
    val lastEditDate: String = this.parse_lastEditDate()

    val tags: Seq[URIWithLabel] = parse_dcat_keywords()
    val themes: Seq[URIWithLabel] = parse_dcat_themes()
    val subthemes: Seq[URIWithLabel] = parse_dct_subthemes()

    val provenance = this.parse_provenance()

    val hasContributor: String = _hasContributor
    val hasFormalityLevel: String = _hasFormalityLevel
    val hasOntologyLanguage: String = _hasOntologyLanguage
    val hasSemanticAssetDistributions: Seq[URIWithValue] = parse_hasSemanticAssetDistributions()
    val hasTasks: Seq[String] = this.parse_hasTasks()
    val owlClasses: Seq[OwlClass] = this.parse_owlClass(onto_url.toString)
    val owlDatatypeProperties: Seq[OwlProperty] = this.parse_owlDatatypeProperties(onto_url.toString)
    val owlObjectProperties: Seq[OwlProperty] = this.parse_owlObject(onto_url.toString)

    OntologyExtendedMeta(
      id,
      rdf_source,
      onto_url,
      prefix,
      namespace,
      concepts,
      imports,
      titles,
      descriptions,
      versions,
      creators,
      publishedBy,
      owners,
      langs,
      creationDate,
      lastEditDate,
      licenses,
      tags,
      themes,
      subthemes,
      provenance,
      hasContributor,
      hasFormalityLevel,
      hasOntologyLanguage,
      hasSemanticAssetDistributions,
      hasTasks,
      owlClasses,
      owlDatatypeProperties,
      owlObjectProperties)

  }

  def parse_id(): String = {
    val result = SPARQL(repo).query("""
      PREFIX owl: <http://www.w3.org/2002/07/owl#>
      PREFIX : <https://w3id.org/italia/onto/ADMS/>
      SELECT DISTINCT ?uri ?acronym
      WHERE {
        ?uri a owl:Ontology .
        OPTIONAL { ?uri :acronym ?acronym . }
      }
    """)
    val value = result.map {x =>
        println(x)
        x.getOrElse("acronym", "").toString()
      }
    if(!value.head.isEmpty) {
      value
        .distinct
        .toSet
        .headOption.getOrElse("")
        .toString()
    }else {
      (rdf_source.getPath.replaceAll(".*/(.*?)\\.[a-z]+$", "$1").trim()).toString
    }

  }

  def parse_hasGroup() {
    SPARQL(repo).query("""
      PREFIX owl: <http://www.w3.org/2002/07/owl#>
      PREFIX : <https://w3id.org/italia/onto/ADMS/>
      SELECT DISTINCT ?hasContributor ?hasFormalityLevel ?hasOntologyLanguage
      WHERE {
        ?uri a owl:Ontology .
        OPTIONAL { ?uri :hasContributor ?hasContributor . }
        OPTIONAL { ?uri :hasFormalityLevel ?hasFormalityLevel . }
        OPTIONAL { ?uri :hasOntologyLanguage ?hasOntologyLanguage . }
      }
    """)
      .map { item =>
          this._hasContributor = item.getOrElse("hasContributor", "").toString()
          this._hasFormalityLevel = item.getOrElse("hasFormalityLevel", "").toString()
          this._hasOntologyLanguage = item.getOrElse("hasOntologyLanguage", "").toString()
      }

//      .distinct
//      .toSet
//      .headOption.getOrElse("")
//      .toString()
  }

  def parse_namespace(default_namespace: String = ""): String = {
    SPARQL(repo).query("""
      SELECT DISTINCT ?uri { ?uri a owl:Ontology .
        FILTER NOT EXISTS {
          FILTER (regex(str(?uri), "align") )
        }
      }
    """)
      //      .map(_.getOrElse("uri", default_namespace).asInstanceOf[String]) // REFACTORIZATION
      .map(_.getOrElse("uri", default_namespace).toString())
      //      CHECK: DISABLED .map { _.replaceAll("^(.*)[/#]$", "$1") } // hack for avoid using both `http://example/` and `http://example`
      .distinct
      .toSet
      .headOption.getOrElse("")
      .toString()
  }

  def parse_onto_url(): URL = {

    var uri: String = ""
    // WORKING VERSION:
    val _onto_url = SPARQL(repo).query("""
      PREFIX : <https://w3id.org/italia/onto/ADMS/>
      SELECT DISTINCT ?uri ?officialURI
      WHERE {
        ?uri a owl:Ontology .
        FILTER NOT EXISTS {
          FILTER (regex(str(?uri), "align") )
        }
        OPTIONAL { ?uri :officialURI ?officialURI .
          FILTER NOT EXISTS {
            FILTER (regex(str(?officialURI), "align") )
          }
        }
      }
    """).toList
      .map {x =>
        uri = x.getOrElse("officialURI", "").asInstanceOf[String]
        if(uri.isEmpty)
          uri = x.getOrElse("uri", "").asInstanceOf[String]

        new URL(uri)
      }
//      .filterNot(uri.trim().equals(""))
//      .map(uri.replaceAll("^(.*?)[#/]$", "$1")) // HACK for threating <http://some/> and <http://some> in the same way
//      .map(new URL(uri))
      .headOption.getOrElse(rdf_source)

    _onto_url
  }

  def parse_concepts(): Set[String] = {
    SPARQL(repo).query("""
      SELECT DISTINCT ?concept 
      WHERE { ?concept a owl:Class . FILTER(!isBlank(?concept)) } 
    """)
      .map(_.getOrElse("concept", "owl:Thing").toString()).toSet
  }

  def parse_imports(): Seq[URIWithLabel] = {
    SPARQL(repo).query("""
      SELECT DISTINCT ?import_uri 
      WHERE {  ?uri a owl:Ontology . ?uri owl:imports ?import_uri . }
    """)
      //      .map(_.get("import_uri").get.asInstanceOf[String]) // REFACTORIZATION
      .map(_.get("import_uri").get.toString())
      .map { item =>
        val uri = item.trim()
        val label = URIHelper.extractLabelFromURI(item)
        val lang = ""
        URIWithLabel(label, uri, lang)
      }
  }

  def parse_titles(): Seq[ItemByLanguage] = {
    SPARQL(repo).query("""
      SELECT DISTINCT * 
      WHERE {  ?uri a owl:Ontology . ?uri rdfs:label ?label . BIND(LANG(?label) AS ?lang) }
    """)
      .map { item =>
        // REFACTORIZATION: item.get("lang").get.asInstanceOf[String], item.get("label").get.asInstanceOf[String]
        ItemByLanguage(item.get("lang").get.toString(), item.get("label").get.toString())
      }
      .toList
  }

  def parse_descriptions(): Seq[ItemByLanguage] = {
    // TODO: case class | Map
    SPARQL(repo).query("""
      SELECT DISTINCT * 
      WHERE {  ?uri a owl:Ontology . ?uri rdfs:comment ?comment . BIND(LANG(?comment) AS ?lang) }
    """)
      .map { item =>
        // REFACTORIZATION : item.get("lang").get.asInstanceOf[String], item.get("comment").get.asInstanceOf[String]
        ItemByLanguage(item.get("lang").get.toString(), item.get("comment").get.toString())
      }
      .toList
  }

  //TODO DA CANCELLARE
//  def parse_creators(): Seq[ItemByLanguage] = {
//      SPARQL(repo).query("""
//        PREFIX dct: <http://purl.org/dc/terms/>
//        SELECT DISTINCT ?lang ?creator
//        WHERE {
//          ?uri a owl:Ontology . ?uri dct:creator ?creator . BIND(LANG(?creator) AS ?lang) .
//        }
//      """)
//        .toList
//        .map { item =>
//          val _creator = item.getOrElse("creator", "").toString()
//          // REFACTORIZATION : .asInstanceOf[String]
//
//          ItemByLanguage(item.getOrElse("lang", "").toString(), item.getOrElse("creator", "").toString())
//          //        Map("label" -> _creator, "lang" -> item.getOrElse("lang", "").toString())
//          // REFACTORIZATION: .asInstanceOf[String])
//        }
//    }

  def parse_creators(): Seq[URIWithLabel] = {
    var tags_container: Seq[URIWithLabel] = Seq[URIWithLabel]()
    var tags_container_tmp: Seq[URIWithLabel] = Seq[URIWithLabel]()
    val concepts = SPARQL(repo).query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        SELECT DISTINCT ?creator_uri
        WHERE {
          ?uri a owl:Ontology .
          ?uri dct:creator ?creator_uri .
          FILTER(isURI(?creator_uri))
        }
        """)

    concepts.foreach { concept =>

      val context = scala.collection.mutable.Map(concept.toSeq: _*).get("creator_uri").get.toString
      tags_container_tmp = this.parse_detail(context)
      tags_container = tags_container_tmp.union(tags_container)
    }
    tags_container
  }

  def parse_versions(): Seq[Version] = {

    // TODO: case class
    SPARQL(repo).query("""
      SELECT DISTINCT * 
      WHERE {  
        ?uri a owl:Ontology . 
        ?uri owl:versionIRI ?version_iri .
    		?uri owl:versionInfo ?version_info . BIND(LANG(?version_info) AS ?lang) .
  		}""")
      .map { item =>

        // TODO: simplify!

        val lang = item.getOrElse("lang", "").toString()
        // REFACTORIZATION: .asInstanceOf[String]

        val _matcher = "^.*?(\\d+\\.\\d+(\\.\\d+)*).*\\s+-\\s+(.*)\\s+-\\s+(.*)$"
        val _vv = item.getOrElse("version_info", "").toString()
        // REFACTORIZATION: .asInstanceOf[String]
        val number = _vv.replaceAll(_matcher, "$1")
        val source_date = _vv.replaceAll(_matcher, "$3")

        val comment = _vv.replaceAll(_matcher, "$4")
        val uri = item.getOrElse("uri", "").toString()
        // REFACTORIZATION: .asInstanceOf[String]
        val version_iri = item.getOrElse("version_iri", "").toString()
        // REFACTORIZATION: .asInstanceOf[String]

//        val comment = Map(lang -> _comment)
        val date = DateHelper.format(DateHelper.parseDate(source_date))

        Version(number, date, lang, comment, uri)
      }

    // TODO: .groupBy { x => x.uri } for collapsing multiple versions (by lang) into one
  }

  def parse_provenance(): Seq[Map[String, Any]] = {
    SPARQL(repo).query("""
      PREFIX dc: <http://purl.org/dc/elements/1.1/>
      PREFIX dct: <http://purl.org/dc/terms/>
      PREFIX prov: <http://www.w3.org/ns/prov#>
      SELECT DISTINCT * {  
        ?uri a owl:Ontology . 
    		OPTIONAL { ?uri dct:license ?license_uri . }
        OPTIONAL { ?uri dct:issued ?date_issued . }
        OPTIONAL { ?uri dct:modified ?date_modified . }
        OPTIONAL { ?uri rdfs:isDefinedBy ?defined_by_uri . }
    		OPTIONAL { ?uri prov:wasDerivedFrom ?derived_from_uri . }
        FILTER ( !isBlank( ?license_uri ) )
        FILTER ( !isBlank( ?date_issued ) )
        FILTER ( !isBlank( ?date_modified ) )
        FILTER ( !isBlank( ?defined_by_uri ) )
        FILTER ( !isBlank( ?derived_from_uri ) )
      }
    """)
  }

  // TODO
  //def parse_publishedBy(): String = ""
  def parse_publishedBy(): Seq[URIWithLabel] = {
    var tags_container: Seq[URIWithLabel] = Seq[URIWithLabel]()
    var tags_container_tmp: Seq[URIWithLabel] = Seq[URIWithLabel]()
    val concepts = SPARQL(repo).query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        SELECT DISTINCT ?publisher_uri
        WHERE {
          ?uri a owl:Ontology .
          ?uri dct:publisher ?publisher_uri .
        }
        """)

    concepts.foreach { concept =>

      val context = scala.collection.mutable.Map(concept.toSeq: _*).get("publisher_uri").get.toString
      tags_container_tmp = this.parse_detail(context)
      tags_container = tags_container_tmp.union(tags_container)
    }
    tags_container
  }

  // TODO
  def parse_owner(): Seq[URIWithLabel] = {
    var tags_container: Seq[URIWithLabel] = Seq[URIWithLabel]()
    var tags_container_tmp: Seq[URIWithLabel] = Seq[URIWithLabel]()

    val concepts = SPARQL(repo).query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        SELECT DISTINCT ?rightsHolder_uri
          WHERE {
            ?uri a owl:Ontology .
            ?uri dct:rightsHolder ?rightsHolder_uri .
        }
        """)

    concepts.foreach { concept =>

      val context = scala.collection.mutable.Map(concept.toSeq: _*).get("rightsHolder_uri").get.toString
      tags_container_tmp = this.parse_detail(context)
      tags_container = tags_container_tmp.union(tags_container)
    }
    tags_container
  }

  def parse_langs(): Seq[String] = {
    SPARQL(repo).query("""
      SELECT DISTINCT ?lang 
      WHERE {  ?uri a owl:Ontology . ?uri rdfs:comment ?comment . BIND(LANG(?comment) AS ?lang) }
    """)
      .map(_.get("lang").getOrElse("").toString())
      // REFACTORIZATION: .asInstanceOf[String])
      .distinct
  }

  def parse_creationDate() = {
    SPARQL(repo).query("""
        PREFIX dct: <http://purl.org/dc/terms/>
        SELECT DISTINCT ?data_creation
        WHERE { ?uri a owl:Ontology . ?uri dct:issued ?data_creation . }
      """)
      .map { item => item.getOrElse("data_creation", "").toString() }
      .headOption.getOrElse("")
  }

  def parse_lastEditDate(): String = {
    SPARQL(repo).query("""
      PREFIX dct: <http://purl.org/dc/terms/>
      SELECT DISTINCT ?date_modified
      WHERE{ ?uri a owl:Ontology . ?uri dct:modified ?date_modified . }
    """)
      //.map(_.getOrElse("last_edit_date", "").asInstanceOf[String])
      .map(_.getOrElse("date_modified", "").toString())
      .headOption.getOrElse("")
  }

  def parse_licenses(): Seq[URIWithLabel] = {
    SPARQL(repo).query("""
      PREFIX dct: <http://purl.org/dc/terms/>
      SELECT DISTINCT ?license_uri {  
        ?uri a owl:Ontology . ?uri dct:license ?license_uri .
      }
    """)
      .map(_.getOrElse("license_uri", "").toString())
      // REFACTORIZTION: .asInstanceOf[String])
      .filterNot(_.equalsIgnoreCase(""))
      .map { item =>
        val uri = item
        val label = URIHelper.extractLabelFromURI(uri)
        val lang = ""
        URIWithLabel(label, uri, lang)
      }
  }

  def parse_dcat_keywords() = {
    SPARQL(repo).query("""
      PREFIX dcat: <http://www.w3.org/ns/dcat#>
      PREFIX owl: <http://www.w3.org/2002/07/owl#>
      SELECT DISTINCT ?keyword ?lang
      WHERE {
        ?uri a owl:Ontology .
        ?uri dcat:keyword ?keyword .
        BIND(LANG(?keyword) AS ?lang)
      }
    """)
      .map { item =>
        /*REFACTORIZATION
        val label = item.getOrElse("keyword", "").asInstanceOf[String].trim()
        val lang = item.getOrElse("lang", "").asInstanceOf[String]
        */
        val label = item.getOrElse("keyword", "").toString().trim()
        val lang = item.getOrElse("lang", "").toString().trim()
        // TODO "uri" AD ATTENDERE DA BONIFICARE
        val uri = s"keywords://${lang}#${label.replaceAll("\\s", "+")}"
        URIWithLabel(label, uri, lang)
      }
  }

  def parse_dcat_themes() = {
    SPARQL(repo).query("""
      PREFIX dcat: <http://www.w3.org/ns/dcat#>
      PREFIX owl: <http://www.w3.org/2002/07/owl#>
      SELECT DISTINCT ?theme ?lang
      WHERE {
        ?uri a owl:Ontology .
        ?uri dcat:theme ?theme .
        BIND(LANG(?theme) AS ?lang)
      }
    """)
      .map { item => item.getOrElse("theme", "").toString() }
      .map { item =>
        val uri = item
        val label = URIHelper.extractLabelFromURI(uri)
        val lang = "" // NOTE: can we assume dcat themes is defined for "eng" language?
        URIWithLabel(label, uri, lang)
      }
  }

  def parse_dct_subthemes() = {
    SPARQL(repo).query("""
      PREFIX dct: <http://purl.org/dc/terms/>
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      SELECT DISTINCT ?subject
      WHERE {
        ?uri a owl:Ontology .
        ?uri dct:subject ?subject .
      }
    """)
      .map { item => item.getOrElse("subject", "").toString() }
      .map { item =>
        val uri = item
        val label = URIHelper.extractLabelFromURI(uri)
        val lang = ""
        URIWithLabel(label, uri, lang)
      }
  }

  def parse_hasSemanticAssetDistributions(): Seq[URIWithValue] = {
    SPARQL(repo).query("""
       PREFIX owl: <http://www.w3.org/2002/07/owl#>
       PREFIX : <https://w3id.org/italia/onto/ADMS/>
       SELECT *
       WHERE {
         ?uri a owl:Ontology .
         ?uri :hasSemanticAssetDistribution ?hasSemanticAssetDistributions .
       }
    """)
      .map { item => item.getOrElse("hasSemanticAssetDistributions", "").toString() }
      .map { item =>
        val uri = item
        val value = URIHelper.extractLabelFromURI(uri)
        URIWithValue(value, uri)
      }
  }

  def parse_hasTasks(): Seq[String] = {
    SPARQL(repo).query("""
       PREFIX owl: <http://www.w3.org/2002/07/owl#>
       PREFIX : <https://w3id.org/italia/onto/ADMS/>
       SELECT *
       WHERE {
         ?uri a owl:Ontology .
         ?uri :hasTask ?hasTasks .
       }
    """)
      .map { item => item.getOrElse("hasTasks", "").toString() }
      .map {item =>
        val uri = item
        new String(uri)
      }
  }

  def parse_detail(context : String): Seq[URIWithLabel] = {

    SPARQL(repo).query(s"""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX dcatapit: <http://dati.gov.it/onto/dcatapit#>
        PREFIX foaf: <http://xmlns.com/foaf/0.1/>
        SELECT *
        WHERE {
          ?agent_uri a dcatapit:Agent .
          ?agent_uri foaf:name ?name .
          FILTER(?agent_uri=<$context>) .
          BIND(LANG(?name) AS ?name_lang)
        }
        """).map { item =>
      val uri = scala.collection.mutable.Map(item.toSeq: _*).get("agent_uri").get.toString
      val label = scala.collection.mutable.Map(item.toSeq: _*).get("name").get.toString
      val lang = scala.collection.mutable.Map(item.toSeq: _*).get("name_lang").get.toString
      URIWithLabel(label, uri, lang)
    }
  }

  def parse_owlClass(namespace : String): Seq[OwlClass] = {

    val nspace = if(namespace.charAt(namespace.length-1).toString.equals("/")) (namespace.substring(0, namespace.lastIndexOf("/"))) else namespace
    SPARQL(repo).query(s"""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX dcatapit: <http://dati.gov.it/onto/dcatapit#>
        PREFIX foaf: <http://xmlns.com/foaf/0.1/>
        SELECT *
        WHERE {

          ?onto a owl:Class .
          FILTER(REGEX(STR(?onto),'https://w3id.org/italia/onto/.*', 'i'))
          ?onto rdfs:isDefinedBy ?defined_by .
          FILTER ( STR(?defined_by) = '$nspace' )

          OPTIONAL {
            { ?onto rdfs:equivalentClass ?equivalent . }
            UNION
            { ?onto owl:equivalentClass ?equivalent . }
          }

          OPTIONAL {
            { ?onto rdfs:subClassOf ?parent_class . }
            UNION
            { ?onto owl:subClassOf ?parent_class . }
            FILTER(!isBlank(?parent_class))
          }
        }
        """).map {item =>
            val definedBy = scala.collection.mutable.Map (item.toSeq: _*).get ("defined_by").get.toString
            val equivalent = scala.collection.mutable.Map (item.toSeq: _*).getOrElse("equivalent", "").toString
            val parentClass = scala.collection.mutable.Map (item.toSeq: _*).getOrElse("parent_class", "").toString
            OwlClass(definedBy, equivalent, parentClass)
        }
  }

  def parse_owlDatatypeProperties(namespace : String): Seq[OwlProperty] = {

    val nspace = if(namespace.charAt(namespace.length-1).toString.equals("/")) (namespace.substring(0, namespace.lastIndexOf("/"))) else namespace
    SPARQL(repo).query(s"""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX dcatapit: <http://dati.gov.it/onto/dcatapit#>
        PREFIX foaf: <http://xmlns.com/foaf/0.1/>
        SELECT *
        WHERE {

          ?onto a owl:DatatypeProperty .
          FILTER(REGEX(STR(?onto),'https://w3id.org/italia/onto/.*', 'i'))
          ?onto rdfs:isDefinedBy ?defined_by .
          FILTER ( STR(?defined_by) = '$nspace' )

          OPTIONAL {
            { ?onto rdfs:equivalentProperty ?equivalentProp . }
            UNION
            { ?onto owl:equivalentProperty ?equivalentProp . }
          }

          OPTIONAL {
            { ?onto rdfs:subPropertyOf ?parent_class . }
            UNION
            { ?onto owl:subPropertyOf ?parent_class . }
            FILTER(!isBlank(?parent_class))
          }
        }
        """).map {item =>
      val definedBy = scala.collection.mutable.Map (item.toSeq: _*).get ("defined_by").get.toString
      val equivalent = scala.collection.mutable.Map (item.toSeq: _*).getOrElse("equivalentProp", "").toString
      val subPropertyOf = scala.collection.mutable.Map (item.toSeq: _*).getOrElse("parent_class", "").toString
      OwlProperty(definedBy, equivalent, subPropertyOf)
    }
  }

  def parse_owlObject(namespace : String): Seq[OwlProperty] = {

    val nspace = if(namespace.charAt(namespace.length-1).toString.equals("/")) (namespace.substring(0, namespace.lastIndexOf("/"))) else namespace
    SPARQL(repo).query(s"""
        PREFIX dct: <http://purl.org/dc/terms/>
        PREFIX dcatapit: <http://dati.gov.it/onto/dcatapit#>
        PREFIX foaf: <http://xmlns.com/foaf/0.1/>
        SELECT *
        WHERE {

          ?onto a owl:ObjectProperty .
          FILTER(REGEX(STR(?onto),'https://w3id.org/italia/onto/.*', 'i'))
          ?onto rdfs:isDefinedBy ?defined_by .
          FILTER ( STR(?defined_by) = '$nspace' )

          OPTIONAL {
            { ?onto rdfs:equivalentProperty ?equivalentProp . }
            UNION
            { ?onto owl:equivalentProperty ?equivalentProp . }
          }

          OPTIONAL {
            { ?onto rdfs:subPropertyOf ?parent_class . }
            UNION
            { ?onto owl:subPropertyOf ?parent_class . }
            FILTER(!isBlank(?parent_class))
          }
        }
        """).map {item =>
      val definedBy = scala.collection.mutable.Map (item.toSeq: _*).get ("defined_by").get.toString
      val equivalent = scala.collection.mutable.Map (item.toSeq: _*).getOrElse("equivalentProp", "").toString
      val subPropertyOf = scala.collection.mutable.Map (item.toSeq: _*).getOrElse("parent_class", "").toString
      OwlProperty(definedBy, equivalent, subPropertyOf)
    }
  }


  // should we shutdown the repository after using it?
  if (!repo.isInitialized()) repo.initialize()
}
