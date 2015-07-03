How to set up jcouchdb in your own app.

# Introduction #

This page describes what you need to use couchdb in your java application with jcouchdb.

# Details #

First of all you need couchdb. jcouchdb is meant to run with couchdb 0.8+ currently only available per couchdb SVN. (the 0.8 version contains some breaking changes)

You'll need some libraries jcouchdb depends on. you can get these libraries either with the [current source download](http://code.google.com/p/jcouchdb/downloads/list) or from SVN by either retrieving the complete project
```
svn co http://jcouchdb.googlecode.com/svn/trunk jcouchdb
```
or by just getting the jar files
```
svn co http://jcouchdb.googlecode.com/svn/trunk/lib/
```

The source download and the SVN both contain Eclipse project files.