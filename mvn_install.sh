#!/bin/bash

mvn install -Dfile=target/kbaselib-0.0.3.jar -DgroupId=it.almawave.linkeddata.kb -DartifactId=kbaselib -Dversion=0.0.3 -Dpackaging=jar 
mkdir -p  ~/.m2/repository/it/almawave/linkeddata/kb/kbaselib_2.11.8/0.0.3/
cp ~/.m2/repository/it/almawave/linkeddata/kb/kbaselib/0.0.3/kbaselib-0.0.3.jar ~/.m2/repository/it/almawave/linkeddata/kb/kbaselib_2.11.8/0.0.3/kbaselib-0.0.3.jar

<<NOTE

In order to avoid this hack, we should find a good configuration for pom.xml, using a combination of {artifactId}, {artifactName}, {version} and something like a {scalaBinaryVersion}

+ sbt import:
crossPaths := false
libraryDependencies += "it.almawave.linkeddata.kb" % "kbaselib" % "0.0.1" changing()
resolvers += Resolver.mavenLocal



NOTE

