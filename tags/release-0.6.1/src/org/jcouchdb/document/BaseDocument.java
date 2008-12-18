package org.jcouchdb.document;

import org.jcouchdb.util.Util;
import org.svenson.AbstractDynamicProperties;
import org.svenson.JSONProperty;

/**
 * Convenience base class for documents. Not abstract so it can be used fully dynamically.
 *
 * @author shelmberger
 *
 */
public class BaseDocument extends AbstractDynamicProperties implements Document
{

    private String id;
    private String revision;

    /* (non-Javadoc)
     * @see org.couchblog.db.Document#getId()
     */
    @JSONProperty(value = "_id", ignoreIfNull = true)
    public String getId()
    {
        return id;
    }

    /* (non-Javadoc)
     * @see org.couchblog.db.Document#setId(java.lang.String)
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /* (non-Javadoc)
     * @see org.couchblog.db.Document#getRevision()
     */
    @JSONProperty(value="_rev",ignoreIfNull = true)
    public String getRevision()
    {
        return revision;
    }

    /* (non-Javadoc)
     * @see org.couchblog.db.Document#setRevision(java.lang.String)
     */
    public void setRevision(String revision)
    {
        this.revision = revision;
    }

    /**
     * Two documents are equal if they have the same id and the same revision.
     *
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Document)
        {
            Document that = (Document)obj;
            return Util.equals(this.getId(), that.getId()) && Util.equals(this.getRevision(),that.getRevision());
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return 17 + Util.safeHashcode(getId()) * 37 + Util.safeHashcode(getRevision()) * 37;
    }
}