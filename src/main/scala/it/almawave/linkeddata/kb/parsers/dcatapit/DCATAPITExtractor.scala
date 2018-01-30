package it.almawave.linkeddata.kb.parsers.dcatapit

/**
 * REFACTORIZATION: each vocabulary needs to have its own DCAT metadata part,
 * so we should be able to parse it and append those informations to the vocabulary descriptor
 * `VocabularyMeta`
 */
class DCATAPITExtractor {

  // TODO

  //  <http://dati.gov.it/onto/controlledvocabulary/POICategoryClassification>
  //      a       skos:ConceptScheme , adms:SemanticAsset , dcatapit:Dataset ;
  //      dct:title "Point of Interest Category Controlled Vocabulary"@en , "Vocabolario Controllato Categorie Punti di Interesse"@it ;
  //      dct:description "Classification of the categories of the point of interest. The classification is based on the first level of the classification proposed by Open Street Map."@en , "Classificazione delle categorie dei punti di interesse. La classificazione è basata su sul primo livello della classificazione proposta da Open Street Map."@it ;
  //      rdfs:comment "Classification of the categories of the point of interest. The classification is based on the first level of the classification proposed by Open Street Map."@en , "Classificazione delle categorie dei punti di interesse. La classificazione è basata su sul primo livello della classificazione proposta da Open Street Map"@it ;
  //      rdfs:label "Point of Interest Category Classification"@en ,  "Classificazione delle categorie dei punti di interesse"@it ;
  //      dct:type <http://purl.org/adms/assettype/CodeList> ;
  //      xkos:numberOfLevels "1";
  //      adms:representationTechnique <http://purl.org/adms/representationtechnique/SKOS> ;
  //      dct:identifier "agid:D.1" ;
  //      dct:rightsHolder <http://spcdata.digitpa.gov.it/browse/page/Amministrazione/agid> ;
  //      dct:creator <http://spcdata.digitpa.gov.it/browse/page/Amministrazione/agid>  , <http://dati.gov.it/data/resource/Amministrazione/td_PCM> ;
  //      dct:publisher <http://spcdata.digitpa.gov.it/browse/page/Amministrazione/agid>  ;
  //      dcat:theme <http://publications.europa.eu/resource/authority/data-theme/EDUC> , <http://publications.europa.eu/resource/authority/data-theme/REGI> ;
  //      dct:subject <http://eurovoc.europa.eu/251915>  ;
  //      dct:conformsTo  <http://dati.gov.it/data/resource/Standard/SKOS> ;
  //      dct:language <http://publications.europa.eu/resource/authority/language/ITA> , <http://publications.europa.eu/resource/authority/language/ENG> ;
  //      dct:accrualPeriodicity <http://publications.europa.eu/resource/authority/frequency/IRREG> ;
  //      rdfs:isDefinedBy <http://dati.gov.it/onto/controlledvocabulary/POICategoryClassification> ;
  //      dcat:keyword "Punti di Interesse"@it, "Categoria"@it , "Settore"@it, "Point of Interest"@en, "Category"@en , "Sector"@en ;
  //      dcat:distribution <http://dati.gov.it/data/resource/Distribution/POICat_RDF_Turtle> , <http://dati.gov.it/data/resource/Distribution/POICat_RDF_XML> , <http://dati.gov.it/data/resource/Distribution/POICat_JSON_LD> , <http://dati.gov.it/data/resource/Distribution/POICat_CSV> ,
  //      <http://dati.gov.it/data/resource/Distribution/POICat_XLSX> ;
  //      dcat:contactPoint <http://dati.gov.it/data/resource/ContactPoint/voc_AgID> ;
  //      dct:issued "2017-10-20"^^xsd:date ;
  //      dct:modified "2018-01-25"^^xsd:date ;
  //      owl:versionInfo "0.2" .

}