package it.almawave.linkeddata.kb.catalog.query

import it.almawave.linkeddata.kb.catalog.VocabularyBox
import java.net.URL
import it.almawave.linkeddata.kb.catalog.SPARQL

class CheckMetadata2Level {

//  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/Licenze/Licenze.ttl")

  val url = new URL("https://raw.githubusercontent.com/italia/daf-ontologie-vocabolari-controllati/master/VocabolariControllati/ClassificazioneTerritorio/Istat-Classificazione-08-Territorio.ttl")
  
  val voc_box = VocabularyBox.parse(url)
  voc_box.start()

  SPARQL(voc_box.repo).query("""
    
  """)
  .foreach{ item => 
    println("")
  }
  
  
  voc_box.stop()


}

/*
 <http://dati.gov.it/Resource/City/Recoaro_Terme>
        a                       clvapit:Feature , clvapit:AddressComponent , clvapit:NamedAddressComponent , clvapit:City ;
        clvapit:hasIdentifier   <http://dati.gov.it/Resource/Identifier/comunecodcat_H214> , <http://dati.gov.it/Resource/Identifier/comuneprog_84> , <http://dati.gov.it/Resource/Identifier/comunealfnum_24084> ;
        clvapit:name            "Recoaro Terme"@it ;
        clvapit:situatedWithin  <http://dati.gov.it/Resource/Province/Vicenza> .
 */
