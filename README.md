kbaselib
==============

The `kbaselib` library contains all the basic core functionalities needed for accessing triplestores and related RDF files.

The library is designed using the RDF4J API, with the aim to provide a simplified uniform access to different triplestores. At the moment is tested using in-memory triplestore and virtuoso, but should be easily compatible with every db supporting RDF4J API. The support for blazegraph is work in progress.
All the functional units are desgned as decorators of the standard RDF4J core component, with the aim of evolving this library to a simple and composable framework, were developers can use only what they actually need.

The main functionalities groups are:

+ extraction / parsing of ontology metadata
+ extraction / parsing of vocabulary metadata
+ low-level (store) API for adding / removing of RDF files
+ SPARQL level API for adding/removing RDF files
+ simple queries over RDF files

Other functionalities such as full-text & faceted search were already tested with Solr/lucene, but not yet fully included in this version.

**NOTE**: the library is still an work-in-progress (alpha version)

## building and local installation

The library can be built and installed locally, with maven:

```bash
mvn clean install \
	-Dfile=target/kbaselib-0.0.2.jar -Dpackaging=jar \
	-DgroupId=it.almawave.linkeddata.kb -DartifactId=kbaselib -Dversion=0.0.2 
```

After the creation and the `mvn install` command, the library will be available to projects, and we can use the library in the usual way.


### maven usage

If the library is available on remote/local maven repositories, we can use it with:

```xml
<dependency>
	<groupId>it.almawave.linkeddata.kb</groupId>
	<artifactId>kbaselib</artifactId>
	<version>0.0.2</version>
</dependency>
```

### local install

+ maven local install
```bash
mvn clean install -Dfile=target/kbaselib-0.0.2.jar -DgroupId=it.almawave.linkeddata.kb -DartifactId=kbaselib -Dversion=0.0.2 -Dpackaging=jar -DgeneratePom=true 

```

+ copying / install library for sbt
we can prepare the jar with sbt conventions for scala
```bash 
# mkdir -p /home/ubuntu/.m2/repository/it/almawave/linkeddata/kb/kbaselib_2.11.8/0.0.2/

cp -R ~/.m2/repository/it/almawave/linkeddata/kb/kbaselib/ ~/.m2/repository/it/almawave/linkeddata/kb/kbaselib_2.11.8/
```

### managing the dependency from extenral sbt projects

A temporary workaround to handle the correct naming convention needed by sbt is to manually create the artifact, for example:

```bash
mkdir -p  ~/.m2/repository/it/almawave/linkeddata/kb/kbaselib_2.11.8/0.0.2/

cp	~/.m2/repository/it/almawave/linkeddata/kb/kbaselib/0.0.2/kbaselib-0.0.2.jar \
	~/.m2/repository/it/almawave/linkeddata/kb/kbaselib_2.11.8/0.0.2/kbaselib-0.0.2.jar
```

The dependency in sbt can be added with:

```
libraryDependencies += "it.almawave.linkeddata.kb" % "kbaselib" % "0.0.1" 
```

where it's important to verify that we are using the library published on the local maven repository for a specific version of scala.

```scala
crossPaths := false

resolvers += Resolver.mavenLocal
```


* * *

## TODO

- [ ] add / improve tests and test coverage
- [ ] add the library to maven central or to a public nexus, in order to simplify the build and installation processes
- [ ] add simple examples of usage in README for calling the basic functionalities
- [ ] import the core catalog part currently copied in `katalod` api
- [ ] add a simple diagram, to explain better the various components and their responsabilities

----

## DOWNLOADS


+ v 0.0.1
	- https://bitbucket.org/awodata/kbaselib/get/d05c14e704b0.zip
	- https://bitbucket.org/awodata/kbaselib/downloads/kbaselib-0.0.1.jar
	- https://bitbucket.org/awodata/kbaselib/downloads/kbaselib-0.0.1_with_dependencies.zip
