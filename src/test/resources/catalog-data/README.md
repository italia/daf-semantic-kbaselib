# Ontologie e Vocabolari Controllati

**TRANNE DOVE DIVERSAMENTE SPECIFICATO, LE ONTOLOGIE SI INTENDONO ANCORA INSTABILI. NEL CORSO DELLE PRIME SETTIMANE DI MARZO 2018 SE NE STABILIZZERANNO ALCUNE DI SEGUITO ELENCATE. SI NOTI INOLTRE CHE GLI URI DI TUTTE LE ONTOLOGIE E VOCABOLARI CONTROLLATI SARANNO CAMBIATI NEI PROSSIMI GIORNI PER ADOTTARE LA NUOVA POLITICA DI URI PERSISTENTI CHE SI E' SCELTA, OVVERO MEDIANTE L'USO DI W3ID.ORG**


Questo è il repository delle ontologie e dei vocabolari controllati sviluppati nell'ambito delle azioni previste dal piano triennale e a supporto del lavoro da svolgere per l'[elenco delle basi di dati chiave](http://elenco-basi-di-dati-chiave.readthedocs.io/it/latest/).
Le ontologie create sono tra loro collegate creando una vera e propria network chiamata **OntoPiA** - a OntoNet system. Le ontologie saranno inserite nel catalogo delle ontologie e vocabolari controllati (si veda la parte [daf-semantics](https://github.com/italia/daf-semantics) e [ontonethub](https://github.com/teamdigitale/ontonethub) per il software relativo al catalogo).


![OntoPiA](OntoPiA.png)

Il repository è suddiviso in due directory:

  + **Ontologie (Ontologies)**: contiene le ontologie OWL, serializzate in RDF/Turtle RDF/XML e JSON-LD. I diagrammi UML delle ontologie sono attualmente in fase di definizione e revisione a seguito dei collegamenti abilitati tra tutte le ontologie. Le ontologie hanno label e commenti sia in inglese (EN), sia in italiano (IT).
  + **Vocabolari controllati (Controlled Vocabularies)**: contiene un elenco dei vocabolari controllati sviluppati anche a supporto delle ontologie.

Il contenuto della directory **Ontologie** è attualmente il seguente:

  + **Eventi IoT (IoT Events)**: Ontologia del profilo applicativo italiano degli eventi IoT (IoT-AP_IT - IoT Italian Application Profile).
  + **Persone (Persons)**: Ontologia del profilo applicativo italiano sulle persone (CPV-AP_IT - Core Person Vocabulary-Italian Application Profile). L'ultima versione dell'ontologia (0.4) è stata definita insieme all'ISTAT nell'ambito dei lavori di ISTAT relativi ai propri registri interni. Nella directory relativa all'ultima versione attuale dell'ontologia vi è anche il file con i relativi allineamenti a ontologie esterne del Web (e.g., FOAF).
  + **Organizzazioni (Organizations)**: Ontologia del profilo applicativo italiano sulle organizzazioni (pubbliche e private) (COV-AP_IT - Core Organization Vocabulary - Italian Application Profile). E' attualmente in fase di definizione il file di allineamneti a ontologie esterne del Web quali Org, RegOrg, Core Public Organization Vocabulary, ecc. **Questa ontologia sarà a breve rifattorizzata per dividersi in tre parti: la parte core per le organizzazioni, l'ontologia specifica per le organizzazioni pubbliche e quella specifica per le organizzazioni private**;
  + **Indirizzi/luoghi (Addresses/Locations)**: Ontologia del profilo applicativo italiano sugli indirizzi e luoghi (CLV-AP_IT - Core Location Vocabulary - Italian Application Profile). Nella directory relativa all'ultima versione attuale dell'ontologia vi è anche il file con i relativi allineamenti a ontologie esterne del Web (e.g., Core Location Vocabulary, AD conforme a INSPIRE, GeoSparql, Geonames, ecc.). **Questa ontologia è in fase di revisione con ISTAT**;
  + **InternetSocialMedia**: Ontologia di supporto. Essa è il profilo applicativo italiano per la modellazione dei social media (account dei social network) e delle informazioni di contatto digitali (sito web istituzionale, indirizzo email, loghi), utilizzati sia nell'ontologia delle organizzazioni che in quella delle persone.
  + **Tempo (Time)**: Primissima versione dell'ontologia di supporto del tempo utilizzata in tutte le ontologie precedenti per cogliere la dimensione temporale dei principali concetti.
  + **Punti di interesse (Points of Interest)**: Ontologia del profilo applicativo italiano sui punti di interesse (Point of Interest - Italian Application Profile (POI-AP_IT)). E' un'ontologia intermedia utilizzata per rappresentare i punti di interesse. Questa sarà specializzata con una serie di ontologie calate sui singoli domini quali strutture ricettive, parcheggi, trasporto pubblico, farmacie, ecc.
  + **Strutture Ricettive (Accommodation)**: Ontologia del profilo applicativo italiano sulle strutture ricettive (Accommodation Italian Application Profile - ACCO-AP_IT).
  + **Unità Di Misura (Measurement Unit)**: Ontologia di supporto per la modellazione delle unità di misura.
  + **Prezzi Offerte e Biglietti (Prices Offers and Tickets)**: Ontologia del profilo applicativo italiano per i prezzi, le offerte i biglietti (Price Offer Ticket - Italian Application Profile -POT-AP_IT. E' un'ontologia di supporto che consente di rappresentare offerte, prezzi e biglietti. Essa può essere utilizzata in svariati contesti come per esempio nell'ambito dei luoghi ed eventi della cultura, nell'ambito delle strutture ricettive o nell'ambito del trasporto, ecc.
  + **Ruoli (Roles)**: Ontologia del profilo italiano per la specifica dei ruoli (Role - Italian Application Profile - RO-AP_IT);
  + **Eventi Pubblici (Public Events)**: Ontologia del profilo applicativo italiano per la rappresentazione degli eventi pubblici (Core Public Event Vocabulary - Italian Application Profile - CPEV-AP_IT). **L'attuale versione dell'ontologia è instabile. Essa sarà modificata anche in base ai risultati che verranno pubblicati entro il prossimo aprile 2018 dal gruppo di lavoro europeo per la definizione del Core Public Event Vocabulary**;
  + **Parcheggi (Car Parks)**: Prima bozza dell'ontologia del profilo applicativo italiano per la rappresentazione dei dati sui parcheggi (Parking Italian Application Profile - Parking-AP_IT);
  + **(L0) (Level0)**: E' un'ontologia top-level che consente di collegare tutte le ontologie sopra elencate abilitando così la network di ontologie.


I **Vocabolari Controllati** sono in generale disponibili in RDF (nelle tre serializzazioni RDF/Turtle, RDF/XML, JSON-LD), in CSV (codifica usata **UTF-8** con separatore **,** (comma)) e in excel. Nell'ambito del DAF, i vocabolari controllati sono dataset e come tali includo i relativi metadati conformi a DCAT-AP_IT.
In particolare, il contenuto della directory Vocabolari Controllati è attualmente il seguente:

  + **Tipi Eventi Pubblici (Types of Public Events)**: E' una classificazione dei possibili tipi di eventi pubblici. La classificazione è allineata a schema.org.
  + **Licenze (Licenses)**: E' la classificazione delle licenze suddivise per tipologia. Questo è il vocabolario controllato da utilizzare per il profilo di metadatazione nazionale DCAT-AP_IT.
  + **Mapping Temi-Sottotemi (Mapping DCAT-AP_IT Themes and Subthemes)**: E' il mapping tra i 13 temi del profilo DCAT-AP_IT e alcune voci del vocabolario Eurovoc da utilizzare per la proprietà [dct:subject](https://linee-guida-cataloghi-dati-profilo-dcat-ap-it.readthedocs.io/it/latest/dataset_elementi_raccomandati.html#sottotema-del-dataset-dct-subject) del profilo DCAT-AP_IT. Il mapping puà essere utilizzato anche in applicativi per guidare l'utente a selezionare i sottotemi Eurovoc in linea con i temi DCAT-AP_IT. Il mapping è basato principalmente sull'analogo mapping utilizzato dall'European Data Portal.
  + **Classificazione Territorio (Territorial Classification)**: E' un dataset RDF allineato con l'ontologia degli Indirizzi/Luoghi suddetta (CLV-AP_IT) basato sul dataset CSV fornito da ISTAT sulla suddivisione Regione/Provincia/Comune e relativi codici.
  + **Classificazione Categorie Punti di Interesse (Categories of Point of Interest)**: E' un dataset, disponibile in CSV, XLSX, e RDF (RDF/XMl, RDF/Turtle JSON-LD) delle categorie di punti di interesse. La categorizzazione è basata sul primo livello di classificazione dei punti di interesse offerta da Open Street Map.
  + **Classificazioni per i servizi pubblici (Classifications for public services)**: La directory contiene tutti vocabolari controllati, ad esclusione di quelli già definiti a livello Europeo per cui si rimanda ai relativi riferimenti, attualmente utilizzati per lo sviluppo del catalogo servizi pubblici.
  + **Classificazioni per le strutture ricettive (Classifications for accommodation facilities)**: La directory contiene tutti i vocabolari controllati specifici per le strutture ricettive (e.g., classificazione a stelle). Al momento la directory contiene la classificazione a stelle e quella sulle tipologie che tiene in considerazione anche alcune classificazioni disponibili a livello regionale.
  + **Classificazioni per le persone (Classifications related to people)**: La directory è strutturata in sotto directory relative al Genere delle Persone e ai Titoli di studio (o Grado di istruzione). Questi vocabolari sono stati sviluppati in collaborazione con ISTAT come emerge dai relativi metadati.
  + **Classificazioni per le organizzazioni (Classifications related to organizations)**: La directory è strutturato in sotto directory relative al momento alle Forme Giuridiche delle organizzazioni. La classificazione è stata predisposta in stretta collaborazione con ISTAT. **A breve saranno pubblicate anche le classificazioni Ateco2007 e COFOG2009**.
  + **Classificazioni per il settore cultura (Classifications for Culture)**: La directory è strutturata in sotto directory relative, al momento, ai soli ambiti disciplinari che possono essere utilizzati nell'ambito dei luoghi ed eventi della cultura. E' stato predisposto un readme che spiega la creazione del vocabolario fatto in collaborazione con alcuni colleghi del MIBACT e la Sapienza Università di Roma.

**Ontologie in fase di sviluppo e non ancora pubblicate** (**Ontologies under development**):
  + **Ontologia dei contratti pubblici (Public Contracts Ontology)**;
  + **Ontologia GTFS**: l'ontologia utilizzata per modellare i dati del trasporto pubblico urbano statico che seguono la specifica GTFS.
  + **Ontologia di supporto per le condizioni di accesso (e.g., Orari d'apertura)**
