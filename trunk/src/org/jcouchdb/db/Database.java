package org.jcouchdb.db;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.svenson.JSON;
import org.svenson.JSONParser;

/**
 * Contains the main interface of working with a couchdb database
 *
 * @author shelmberger
 *
 */
public class Database
{
    private JSON JSON = new JSON();

    static final String DOCUMENT_TYPE_PATH = ".rows[].value";

    protected static Logger log = Logger.getLogger(Database.class);

    private String name;

    private Server server;

    public Database(String host, String name)
    {
        this(new ServerImpl(host), name);
    }

    public Database(String host, int port, String name)
    {
        this(new ServerImpl(host, port), name);
    }

    public Database(Server server, String name)
    {
        this.server = server;
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public Server getServer()
    {
        return server;
    }

    /**
     * Returns the {@link DatabaseStatus} of the current database
     *
     * @return
     */
    public DatabaseStatus getStatus()
    {
        Response resp = server.get("/" + name + "/");
        if (!resp.isOk())
        {
            throw new DataAccessException("error getting database status for database " + name +
                ": ", resp);
        }
        return resp.getContentAsBean(DatabaseStatus.class);
    }

    /**
     * Triggers database compaction.
     *
     */
    public void compact()
    {
        Response resp = server.get("/" + name + "/_compact");
        if (!resp.isOk())
        {
            throw new DataAccessException("error getting database status for database " + name +
                ": ", resp);
        }
    }

    /**
     * Returns the design document with the given id.
     *
     * @param id
     * @return
     */
    public DesignDocument getDesignDocument(String id)
    {
        return getDocument(DesignDocument.class, DesignDocument.extendId(id));
    }

    /**
     * Returns the document with the given id and converts it to the given class.
     *
     * @param <T>   type
     * @param cls   runtime class info
     * @param docId document id
     */
    public <T> T getDocument(Class<T> cls, String docId)
    {
        return getDocument(cls,docId,null);
    }

    /**
     * Returns the document with the given id and converts it to the given class with
     * the given configured JSONParser
     *
     * @param <T>       type
     * @param cls       runtime class info
     * @param docId     document id
     * @param parser    configured parser
     *
     * @see JSONParser#addTypeHint(String, Class)
     * @see JSONParser#setTypeHints(Map)
     * @see JSONParser#setTypeMapper(org.jcouchdb.json.TokenInspector)
     * @return
     */
    public <T> T getDocument(Class<T> cls, String docId, JSONParser parser)
    {
        Response resp = server.get("/" + name + "/" + docId);
        if (resp.getCode() == 404)
        {
            throw new NotFoundException("document not found", resp);
        }
        else if (!resp.isOk())
        {
            throw new DataAccessException("error getting document " + docId + ": ", resp);
        }
        if (parser != null)
        {
            resp.setParser(parser);
        }
        return resp.getContentAsBean(cls);
    }

    /**
     * Creates the given document and updates  the document's id and revision properties. If the
     * document has an id property, a named document will be created, else the id will be generated by the server.
     * assigned.
     *
     * @param doc   Document to create.
     * @throws IllegalArgumentException if the document already had a revision set
     */
    public void createDocument(Object doc)
    {
        if (DocumentHelper.getRevision(doc) != null)
        {
            throw new IllegalArgumentException("Newly created docs can't have a revision ( is = " +
                DocumentHelper.getRevision(doc) + " )");
        }

        createOrUpdateDocument(doc);
    }

    public List<DocumentInfo> bulkCreateDocuments(Collection<Document> documents)
    {
        Map<String,Collection<Document>> wrap = new HashMap<String, Collection<Document>>();
        wrap.put("docs", documents);

        final String json = JSON.forValue(wrap);
        Response resp = server.post("/" + name + "/_bulk_docs", json);

        JSONParser parser = new JSONParser();
        parser.addTypeHint(".new_revs[]", DocumentInfo.class);
        resp.setParser(parser);
        Map m = resp.getContentAsBean(HashMap.class);

        if (m.get("ok") != null)
        {
            return (List<DocumentInfo>) m.get("new_revs");
        }
        else
        {
            throw new DataAccessException("Error bulk creating documents", resp);
        }
    }

    /**
     * Creates or updates given document and updates the document's id and revision properties. If the
     * document has an id property, a named document will be created, else the id will be generated by the server.
     * assigned.
     *
     * @param doc   Document to create.
     */
    public void createOrUpdateDocument(Object doc)
    {
        Response resp;
        String id = DocumentHelper.getId(doc);
        final String json = JSON.forValue(doc);
        if (id == null)
        {
            resp = server.post("/" + name + "/", json);
        }
        else
        {
            resp = server.put("/" + name + "/" + id, json);
        }

        if (resp.getCode() == 412)
        {
            throw new UpdateConflictException("error creating document "+json, resp);
        }
        else if (!resp.isOk())
        {
            throw new DataAccessException("error creating document " + json, resp);
        }
        DocumentInfo info = resp.getContentAsBean(DocumentInfo.class);

        if (id == null)
        {
            DocumentHelper.setId(doc, info.getId());
        }
        DocumentHelper.setRevision(doc, info.getRevision());
    }

    /**
     * Updates given document and updates the document's revision property.
     *
     * @param doc   Document to create.
     * @throws IllegalArgumentException if the document had no revision property
     */
    public void updateDocument(Object doc)
    {
        if (DocumentHelper.getId(doc) == null)
        {
            throw new IllegalStateException("id must be set for updates");
        }
        if (DocumentHelper.getRevision(doc) == null)
        {
            throw new IllegalStateException("revision must be set for updates");
        }

        createOrUpdateDocument(doc);
    }

    /**
     * Lists all documents as maps.
     * @return
     */
    public ViewResult<Map> listDocuments()
    {
        return listDocuments(null,null);
    }

    /**
     * Lists all documents as maps.
     * @param options query options
     * @return
     */
    public ViewResult<Map> listDocuments(Options options)
    {
        return listDocuments(options,null);
    }

    public ViewResult<Map> listDocuments(Options options, JSONParser parser)
    {
        return queryViewInternal("_all_docs", Map.class, options, parser);
    }

    /**
     * Queries the view with the given name and converts the received views
     * to the given type
     * @param <T>       type
     * @param viewName  view name
     * @param cls       runtime type information
     * @return
     */
    public <T> ViewResult<T> queryView(String viewName, Class<T> cls)
    {
        return queryView(viewName, cls, null);
    }

    /**
     * Queries the view with the given name and converts the received views
     * to the given type
     * @param <T>       type
     * @param viewName  view name
     * @param cls       runtime type information
     * @param options   query options
     * @return
     */
    public <T> ViewResult<T> queryView(String viewName, Class<T> cls, Options options)
    {
        return queryView(viewName, cls, options, null);
    }

    /**
     * Queries the view with the given name and converts the received views
     * to the given type
     * @param <T>       type
     * @param viewName  view name
     * @param cls       runtime type information
     * @param options   query options
     * @param parser    configured JSON Parser
     * @return
     */
    public <T> ViewResult<T> queryView(String viewName, Class<T> cls, Options options, JSONParser parser)
    {
        return queryViewInternal("_view/" + viewName, cls, options, parser);
    }

    /**
     * Executes the given map / reduce functions and convert the response to a view result of the given class.
     * @param <T>   type
     * @param cls   runtime type info
     * @param fn    map / reduce function as valid JSON string (e.g. <code>{ "map" : "function(doc) { emit(null,doc);" }</code>
     * @return
     */
    public <T> ViewResult<T> queryAdHocView(Class<T> cls, String fn)
    {
        return queryAdHocView(cls, fn, null, null);
    }

    /**
     * Executes the given map / reduce functions and convert the response to a view result of the given class.
     * @param <T>       type
     * @param cls       runtime type info
     * @param fn        map / reduce function as valid JSON string (e.g. <code>{ "map" : "function(doc) { emit(null,doc);" }</code>
     * @param options   query options
     * @return
     */
    public <T> ViewResult<T> queryAdHocView(Class<T> cls, String fn, Options options)
    {
        return queryAdHocView(cls, fn, options, null);
    }

    /**
     * Executes the given map / reduce functions and convert the response to a view result of the given class.
     * @param <T>       type
     * @param cls       runtime type info
     * @param fn        map / reduce function as valid JSON string (e.g. <code>{ "map" : "function(doc) { emit(null,doc);" }</code>
     * @param options   query options
     * @param parser    configured {@link JSONParser}
     * @return
     */
    public <T> ViewResult<T> queryAdHocView(Class<T> cls, String fn, Options options, JSONParser parser)
    {
        if (cls == null)
        {
            throw new IllegalArgumentException("class cannot be null");
        }

        String uri = "/"+name+"/_temp_view";

        if (options != null)
        {
            uri += options.toQuery();
        }

        if (log.isDebugEnabled())
        {
            log.debug("querying view " + uri);
        }

        Response resp = server.post(uri,fn);
        if (!resp.isOk())
        {
            throw new DataAccessException("error querying view", resp);
        }

        if (parser == null)
        {
            parser = new JSONParser();
        }
        parser.addTypeHint(DOCUMENT_TYPE_PATH, cls);
        resp.setParser(parser);
        return resp.getContentAsBean(ViewResult.class);
    }

    /**
     * Internal view query method.
     *
     * @param <T>
     * @param viewName
     * @param cls
     * @param options
     * @param parser
     * @return
     */
    private <T> ViewResult<T> queryViewInternal(String viewName, Class<T> cls, Options options, JSONParser parser)
    {
        if (viewName == null)
        {
            throw new IllegalArgumentException("view name cannot be null");
        }
        if (cls == null)
        {
            throw new IllegalArgumentException("class cannot be null");
        }

        String uri = "/" + this.name + "/" + viewName;

        if (options != null)
        {
            uri += options.toQuery();
        }

        if (log.isDebugEnabled())
        {
            log.debug("querying view " + uri);
        }

        Response resp = server.get(uri);
        if (!resp.isOk())
        {
            throw new DataAccessException("error querying view", resp);
        }

        if (parser == null)
        {
            parser = new JSONParser();
        }
        parser.addTypeHint(DOCUMENT_TYPE_PATH, cls);
        resp.setParser(parser);
        return resp.getContentAsBean(ViewResult.class);
    }
}
