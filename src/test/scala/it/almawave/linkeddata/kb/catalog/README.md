# TODO (for configurations)

IDEA: guess if not specified!

IDEA: 

1) catalog loader fetches ontologies / vocabularies files from the remote repository (or from a local cache)
2) an RDF metadata extractor extracts specific metadata for ontology / vocabulary
3) from the metadata, all ontologies and vocabularies are added to repository and ontonethub

	3.a) during metadata extraction, we can describe all the imports and dependencies for both ontology and vocabulary.
		This way we could create a small self-consistent in-memory RDFFileRepository, containing the source and the dependencies.
		This in-memory repositories could be federated, and can be used for exposing specific API for ontologies and vocabularies. 


TODO: transcribe conventional rules for creating metadata

## ontology

type: "ontology"
id: "COV-AP_IT"

uri: "http://dati.gov.it/onto/covapit"

prefix: covapit
namespace: "http://dati.gov.it/onto/covapit#"

path: "/Ontologie/Organizzazioni/latest/COV-AP_IT.ttl"
mime: text/turtle # servirà slo per import!

contexts: [ "http://dati.gov.it/onto/covapit" ] 

source: ${remote}"/Ontologie/Organizzazioni/latest/COV-AP_IT.ttl"
cache: ${local}"/Ontologie/Organizzazioni/latest/COV-AP_IT.ttl"

alignments: ???

SEE: for extracting 
<http://dati.gov.it/onto/covapit> a owl:Ontology ; rdfs:isDefinedBy <http://dati.gov.it/onto/covapit> .
	

IDEA: use paths for multiple files, or path for single source!




## vocabulary

type: "vocabulary"
id: "POICategoryClassification"

mime: text/turtle # servirà slo per import!
namespace: "http://dati.gov.it/onto/covapit#"
contexts: [ "https://www.dati.gov.it/onto/controlledvocabulary/POICategoryClassification" ]

source: ${remote}"/Ontologie/Organizzazioni/latest/COV-AP_IT.ttl"
cache: ${local}"/Ontologie/Organizzazioni/latest/COV-AP_IT.ttl"

uri: "http://dati.gov.it/onto/controlledvocabulary/POICategoryClassification"

alignments: ???

SEE:	<http://dati.gov.it/onto/controlledvocabulary/POICategoryClassification> a skos:ConceptScheme ; rdfs:isDefinedBy <http://dati.gov.it/onto/controlledvocabulary/POICategoryClassification> .


















