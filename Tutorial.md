How to use jcouchdb in your java applications.

# Introduction #

Jcouchdb should support all features of CouchDB 0.8.

# The Database class #

The [org.jcouchdb.db.Database](http://jcouchdb.googlecode.com/svn/trunk/src/org/jcouchdb/db/Database.java) class is your main point of interaction with couchdb.
Let's see some simple code:

```
    // create a database object pointing to the database "mycouchdb" on the local host    
    Database db = new Database("localhost", "mycouchdb");

    // create a hash map document with two fields    
    Map<String,String> doc = new HashMap<String, String>();
    doc.put("foo", "value for the foo attribute");
    doc.put("bar", "value for the bar attribute");

    // create the document in couchdb
    db.createDocument(doc);
```

This creates a simple Document with two fields based on a HashMap. Note that the database has to already exist for it to work. Databases can be created via Futon or with the [org.jcouchdb.db.ServerImpl](http://jcouchdb.googlecode.com/svn/trunk/src/org/jcouchdb/db/ServerImpl.java) class.

## Documents ##

Jcouchdb offers many different ways of using java classes as !CouchDB documents.
You can work totally schema-free and without any type constraint and use a Java object graph composed of list and map instances or you can use your own Java classes with strict type constraints or you can have something in between. it's your choice.

Jcouchdb depends on the [svenson JSON library](http://code.google.com/p/svenson/) which in fact used to be a part of jcouchdb.

To fully understand the possibilities of working with svenson you should go and read the [svenson documentation](http://code.google.com/p/svenson/w/list). Here I will only show you the jcouchdb specific parts.

First there is the [org.jcouchdb.document.Document](http://jcouchdb.googlecode.com/svn/trunk/src/org/jcouchdb/document/Document.java) interface which you can implement in your Documents.
```
package org.jcouchdb.document;

import org.svenson.JSONProperty;
/**
 * Interface for Documents used with jcouchdb.
 *
 * You don't actually have to implement Document, but your class needs
 * to be able to be fed both "_id" and "_rev" properties -- but without
 * those properties you cannot really work with couchdb anyway.
 *
 * @author shelmberger
 *
 */
public interface Document
{

    @JSONProperty( value = "_id", ignoreIfNull = true)
    String getId();

    void setId(String id);

    @JSONProperty( value = "_rev", ignoreIfNull = true)
    String getRevision();

    void setRevision(String revision);

}
```

There is also a convenience base class [org.jcouchdb.document.BaseDocument](http://jcouchdb.googlecode.com/svn/trunk/src/org/jcouchdb/document/BaseDocument.java) which implements
the Document interface and [org.svenson.DynamicProperties](http://svenson.googlecode.com/svn/trunk/src/org/svenson/DynamicProperties.java) which allows its subclasses to have both java properties and additional arbritrary properties.

### Design documents ###

[org.jcouchdb.document.DesignDocument](http://jcouchdb.googlecode.com/svn/trunk/src/org/jcouchdb/document/DesignDocument.java) lets you work with CouchDB design documents. There is also a small utility class [org.jcouchdb.util.CouchDBUpdater](http://jcouchdb.googlecode.com/svn/trunk/src/org/jcouchdb/util/CouchDBUpdater.java) that lets you keep the design document definitions outside of CouchDB and update CouchDB with them.

## Views ##

The database class also offers ways to query views and ad hoc views.

```
import org.jcouchdb.db.Options;
import org.jcouchdb.document.ViewResult;

    ...

    // assume db to be a jcouchdb database object
    ViewResult<Map> result = db.queryView("myDesignDocId/myViewId", Map.class, null, null);

    // query with options
    ViewResult<Map> result2 = db.queryView("myDesignDocId/myViewId", Map.class, 
        new Options().count(10), null);

    // you can also specify a svenson JSONParser to parse the results with. This gives you optimal control over how the JSON is turned into java objects
    ViewResult<Map> result2 = db.queryView("myDesignDocId/myViewId", null, parser);

    // query a view including its documents and convert the documents to FooDocument
    ViewAndDocumentsResult<Object,FooDocument> result = db.queryViewAndDocuments("foo/byValue", Object.class, FooDocument.class, null, null);

    // query views by keys allows you to specify a list of keys you want to fetch
    // Here we fetch the keys "doc-1" and "doc-2" out of the view "foo/byValue"
    // (The key is the first argument given to the emit in your map function)
    ViewResult<FooDocument> result = db.queryViewByKeys("foo/byValue", FooDocument.class, Arrays.asList("doc-1","doc-2"), null, null);


```

The view result gives you access to the total rows and offset of the query and to the converted documents.

See the [svenson JSONParser documentation](http://code.google.com/p/svenson/wiki/ParsingJSON) of how to use the JSONParser in svenson.

## Examples in tests ##

Jcouchdb comes with a set of unit tests which you can also take a look at to see examples of how to use jcouchdb (most importantly [org.jcouchdb.db.LocalDatabaseTestCase](http://jcouchdb.googlecode.com/svn/trunk/test/org/jcouchdb/db/LocalDatabaseTestCase.java) which runs some integration tests against local CouchDB server.

## Attachments ##
The attachment mechanism mirrors the functionality present in couchdb itself.
You will only be able to indirectly use the data property contained in {@link Attachment} to create
attachments inlined with the document. When you query a document with attachments, the attachments will
have a null data property and the stub property will be set to true.

This limitation has its origin in the way couchdb works and is deliberately kept that way to not introduce additional queries.

To get the actual content of the attachment back, you have to use the
[Database#getMethod(String,String)](http://fforw.de/static/jcouchdb-javadoc/org/jcouchdb/db/Database.html#getAttachment(java.lang.String,%20java.lang.String)).

## CouchdDBUpdater ##

In 0.7.1 I've added a small tool class that can help you update your design documents / views while being able to edit them in your favourite editor.

You just have to set up a directory structure with all your design documents and views in them, like this:

```

couchdb-views/
    foo/
        byName.map.js
        byName.reduce.js

```

you need one root directory in which you create a folder per design document. the name of the folder will be the name of the design document. Then you put a ".map.js" file for every map function and a ".reduce.js" for every reduce function and do something like this.

```
import org.jcouchdb.util.CouchDBUpdater;
    ...
    // assume db to be a Database object pointing to the database you want to update and
    // dir a File object pointing to the directory structure desribed above.
    CouchDBUpdater updater = new CouchDBUpdater();
    updater.setDatabase(db);
    updater.setDesignDocumentDir(dir);
    updater.updateDesignDocuments();
```

The updater will go through every definition he finds and check if the design document exists in the couchdb database exactly like he found it. if not, he will updated the design document.