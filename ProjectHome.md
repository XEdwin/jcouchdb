# jcouchdb #

jcouchdb is a java5 couchdb driver using the [svenson JSON library](http://code.google.com/p/svenson/).

It offers features to support the full range from totally dynamic parsing to parsing into concrete java POJOs, including a mix in between.

jcouchdb is in the early stages of development but there's a test suite proving that it does at least most of the couchdb operations.

## Mailing List ##

For discussion about jcouchdb or help with jcouchdb, you can contact [the mailing list](http://groups.google.com/group/jcouchdb/).

## Hood Example Application ##

On the occasion of presenting CouchDB and jcouchdb at my place of work, I got around to finally create a small example application that is now downloadable as sneak preview. There need to be bugs fixed, features implemented and lots of documentation to be added, but it kind of works.

It's called "Hood" for neighbourhood and allows you to mark places or people around a place of activity of yours, called hood. it is meant to foster collaboration / tips on local places etc.

It's Spring Web Application demonstrating some techniques of working with jcouchdb. It's an eclipse WTP/Spring IDE project with all dependencies you need besides couchdb and tomcat or another servlet container.

The current version, beta 2, still lacks some features but is a lot closer to the goal. The design is a lot nicer now, lots of documentation and bugfixed added.

You can check out the [Hood API Doc](http://fforw.de/static/hood-javadoc/) to get a basic overview.

If you want to follow the development, the sources can be checked out anonymously via SVN at

```
http://jcouchdb.googlecode.com/svn/examples/hood
```

## Documentation ##

Apart from the [jcouchdb tutorial](http://code.google.com/p/jcouchdb/wiki/Tutorial) and the [svenson documentation](http://code.google.com/p/svenson/w/list), you can also
browse the [jcouchdb javadoc](http://fforw.de/static/jcouchdb-javadoc/).

## Version system ##

With the current release, I switched to a different versioning system containing the release of couchdb the jcouchdb version is meant to be run with followed by a release counter.

## Maven ##

Now that we're synced you can just add a maven dependency to your pom.xml like this:

```
    <dependency>
        <groupId>com.google.code.jcouchdb</groupId>
        <artifactId>jcouchdb</artifactId>
        <version>0.11.0-1</version>
    </dependency>
```

## Changelog ##


Changes from 0.11.0-1 to 1.0.1:
  * meant to run against the new CouchDB 1.0.1
  * add https option for ServerImpl
  * bugfixes

Changes from 0.10.0-3 to 0.11.0-1:
  * removed old stats API call
  * support for new CouchDB 0.11 feature linked documents via @JSONReference and ResultAssembler

Changes from 0.10.0-2 to 0.10.0-3:

  * Fix encoding inconsistencies
  * Use DocumentHelper for bulkDeleteDocuments, change signature to List<? extends Object>
  * Properly escape spaces and other characters in URLs.

Changes from 0.10.0-1 to 0.10.0-2:

  * added support for _uuids and replication
  * support use cases with app global JSON config. See Hood Beta 2 for details.
  * Fixed serious bug regarding JSONParser runs influencing each others JSON config in some situations._

Changes from 0.9.1-2 to 0.10.0-1:

  * compatibility with couchdb 0.10.0: attachment revpos, new list function format
  * add ResourceSync to sync folders on disk with document attachments, MediaTypeUtil to determine media type by name.
  * add Database.setJsonGenerator(JSON) to suport configuration of JSON output
  * minor bugfixes

Changes from 0.9.1-1 to 0.9.1-2:
  * Added JarBasedCouchDBUpdater that can update views from a directory or jar in the classpath
  * support for _show and_list
  * support for _stats
  * now uses slf4j as logging API
  * switched to httpcomments http-client 4.0 from the old commons-httpclient-3.x
  * now available via Maven
  * bugfix: added DocumentValidationException to access reason of validation errors.
  * bugfix: don't escape startkey\_docid, endkey\_docid anymore
  * support for all\_or\_nothing bulk creation, support for bulk delete. Thanks to Daniel Trümper for submitting the patches_

## Version 0.7 breaks API compatibility ##

I regret to have to inform you that because of the new features for the upcoming 0.9 release, which made the old way of keeping around methods which do not accept Options and JSONParser parameters too bothersome, I removed those methods in the old API and added no new ones in jcouchdb 0.7.

For your old code to work, you must now pass null where you don't haven Options and/or a JSONParser.


## Dependencies ##

The project currently compiles against the following jars which can also be checked out from SVN.
```
commons-beanutils.jar
commons-codec-1.3.jar
commons-httpclient-3.1.jar
commons-io-1.3.1.jar
commons-logging-1.1.jar
easymock-2.3.jar
hamcrest-all-1.1.jar
junit-4.4.jar
log4j-1.2.14.jar
svenson-1.2.8.jar
```
