
# esempio "geografico" (con CLV-AP_IT)

DROP GRAPH <test://VocabolariControllati/ClassificazioneTerritorio/Istat-Classificazione-08-Territorio>
;

LOAD <file:///C:/Users/Al.Serafini/repos/DAF/daf-ontologie-vocabolari-controllati/VocabolariControllati/ClassificazioneTerritorio/Istat-Classificazione-08-Territorio.ttl>
INTO GRAPH <test://VocabolariControllati/ClassificazioneTerritorio/Istat-Classificazione-08-Territorio>
;

LOAD <file:///C:/Users/Al.Serafini/repos/DAF/daf-ontologie-vocabolari-controllati/Ontologie/IndirizziLuoghi/latest/CLV-AP_IT.ttl>
INTO GRAPH <test://VocabolariControllati/ClassificazioneTerritorio/Istat-Classificazione-08-Territorio>
;

----

# esempio Licenze (SKOS)

DROP GRAPH <test://VocabolariControllati/licences>
;

LOAD <file:///C:/Users/Al.Serafini/repos/DAF/daf-ontologie-vocabolari-controllati/VocabolariControllati/licences/licences.ttl>
INTO GRAPH <test://VocabolariControllati/licences>
;







