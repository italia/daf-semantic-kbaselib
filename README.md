kbaselib
==============

[ README last update: 01/03/2018  ]

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

----

## building / usage of the library

At the moment the library is not (yet) available on a public maven repository or on the maven central, so it's important to publish it locally at least on maven (a workaround for sbt is suggested below), before building any project with it in the standard way.

A possible alternative way of re-using it is by including the jar directly in the project. The links to downloadable jars can be found at the end of this document.

### mave local installation

In order to have the library available as a dependency from other maven/sbt project, we should install it at least locally:

```bash
mvn clean install \
	-Dfile=target/kbaselib-0.0.4.jar -Dpackaging=jar \
	-DgroupId=it.almawave.linkeddata.kb -DartifactId=kbaselib -Dversion=0.0.4 
```

After the creation and the `mvn install` command, the library will be available to projects, and we can use the library in the usual way.


### sbt local installation

A temporary workaround to handle the correct naming convention needed by sbt is to manually create the artifact, for example:

```bash
# mkdir -p /home/ubuntu/.m2/repository/it/almawave/linkeddata/kb/kbaselib_2.11.8/0.0.4/

cp -R ~/.m2/repository/it/almawave/linkeddata/kb/kbaselib/ ~/.m2/repository/it/almawave/linkeddata/kb/kbaselib_2.11.8/
```



### maven usage

If the library is available on remote/local maven repositories, we can use it with:

```xml
<dependency>
	<groupId>it.almawave.linkeddata.kb</groupId>
	<artifactId>kbaselib</artifactId>
	<version>0.0.4</version>
</dependency>
```

### sbt usage

The dependency in sbt can be added with:
```scala
libraryDependencies += "it.almawave.linkeddata.kb" % "kbaselib" % "0.0.4"
```

where it's important to verify that we are using the library published on the local maven repository for a specific version of scala, so it's important to check also the following part of the `build.sbt` file for sbt:

```scala
crossPaths := false

resolvers += Resolver.mavenLocal
```

* * *

## TODO

- [ ] add / improve tests and test coverage
- [x] import the core catalog part currently copied in `katalod` api
- [x] add VOWL library, enabling simple visualizations (for ontologies)
- [x] add DCAT metadata parsing, improving navigation (for vocabularies)
- [ ] add DCAT metadata parsing, improving navigation (for ontologies, if it makes sense)
- [ ] add simple examples of usage in README for calling the basic functionalities
- [ ] add a simple diagram, to explain better the various components and their responsabilities
- [ ] publishing the library on an available maven repository (nexus, etc), in order to simplify the build and installation processes

* * *

## DOWNLOADS

+ v 0.0.4
	- TODO

+ v 0.0.2
	- [kbaselib-0.0.2.jar](https://bitbucket.org/awodata/kbaselib/downloads/kbaselib-0.0.2.jar)
	- [kbaselib-0.0.2_with_dependencies.zip](https://bitbucket.org/awodata/kbaselib/downloads/kbaselib-0.0.2_with_dependencies.zip)
	- [repo](https://bitbucket.org/awodata/kbaselib/get/e07936e6a335.zip)

+ v 0.0.1
	- [kbaselib-0.0.1.jar](https://bitbucket.org/awodata/kbaselib/downloads/kbaselib-0.0.1.jar)
	- [kbaselib-0.0.1_with_dependencies.zip](https://bitbucket.org/awodata/kbaselib/downloads/kbaselib-0.0.1_with_dependencies.zip)
	- [repo](https://bitbucket.org/awodata/kbaselib/get/d05c14e704b0.zip)
	

