
# esempio "geografico" (con CLV-AP_IT)

DROP GRAPH <test://VocabolariControllati/ClassificazioneTerritorio/Istat-Classificazione-08-Territorio>
;

LOAD <file:///C:/Users/Al.Serafini/repos/DAF/dati-semantic-assets/VocabolariControllati/ClassificazioneTerritorio/Istat-Classificazione-08-Territorio.ttl>
INTO GRAPH <test://VocabolariControllati/ClassificazioneTerritorio/Istat-Classificazione-08-Territorio>
;

LOAD <file:///C:/Users/Al.Serafini/repos/DAF/dati-semantic-assets/Ontologie/IndirizziLuoghi/latest/CLV-AP_IT.ttl>
INTO GRAPH <test://VocabolariControllati/ClassificazioneTerritorio/Istat-Classificazione-08-Territorio>
;

----

# esempio Licenze (SKOS)

DROP GRAPH <test://VocabolariControllati/licences>
;

LOAD <file:///C:/Users/Al.Serafini/repos/DAF/dati-semantic-assets/VocabolariControllati/licences/licences.ttl>
INTO GRAPH <test://VocabolariControllati/licences>
;







