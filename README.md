kbaselib
==============

## building and local installation

The library can be built and installed locally, with maven:

```
mvn clean install \
	-Dfile=target/kbaselib-0.0.1.jar -Dpackaging=jar \
	-DgroupId=it.almawave.linkeddata.kb -DartifactId=kbaselib -Dversion=0.0.1 
```

After the creation and the `mvn install` command, the library will be available to projects, and we can use the library in the usual way.

**TODO**: in order to simplify the builds and avoiding the local installtion, at some point the library will be available using nexus or directly on maven central.

### maven usage

If the library is available on remote/local maven repositories, we can use it with:

```
<dependency>
	<groupId>it.almawave.linkeddata.kb</groupId>
	<artifactId>kbaselib</artifactId>
	<version>0.0.1</version>
</dependency>
```

### managing the dependency from extenral sbt projects

A temporary workaround to handle the correct naming convention needed by sbt is to manually create the artifact, for example:

```
cp ~/.m2/repository/it/almawave/linkeddata/kb/kbaselib/0.0.1/kbaselib-0.0.1.jar ~/.m2/repository/it/almawave/linkeddata/kb/kbaselib_2.11.8/0.0.1/kbaselib-0.0.1.jar
```

The dependency in sbt can be added with:

```
libraryDependencies += "it.almawave.linkeddata.kb" % "kbaselib" % "0.0.1" 
```

where it's important to verify that we are using the library published on the local maven repository for a specific version of scala.

```
crossPaths := false

resolvers += Resolver.mavenLocal
```

