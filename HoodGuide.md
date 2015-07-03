The project contains eclipse project files . So the easiest way to get an overview over hood is to just import it into Eclipse with "File / Import.. -> Existing Projects into Workspace".

Here is a little overview over the files and directories in the project. if you don't
want to use eclipse you might like the ant build script provided.

```
WebContent
    WEB-INF
        config
            couchdb-config.xml      CouchDB server and database config
            couchdb-services.xml    CouchDBUpdater config that pushes the source views to
                                    the CouchDB server
            security-config.xml     Spring security
            services-config.xml     svenson JSON config, object services
            webmvc-config.xml       Spring WebMVC config. 
 
        designdocs                  source for CouchDB views
            ...
        lib                         runtime dependencies as jar files
            ...
        tags
            hood-fn.tld     Some JSP EL functions for hood
            link.tag        A link tag to avoid <c:url value=""/> for simple links
            page.tag        general page layout as JSP tag file
        views               contains the JSP views
            delObject.jsp   Delete Object?
            hood.jsp        main view
            login.jsp       login view
            newObject.jsp   new object view
            objects.jsp     non-JS view for objects
        web.xml             Servlet API web.xml file 
    image                   Images
        ... 
    index.jsp               redirects to the hood view
    script
        firebug-emu.js      emulates firebug if not present
        hood.js             main app script
        jquery-1.3.2.js     jquery 1.3.2
        json2.js            JSON api by Douglas Crockford (for those browser who don't do it natively)
    style                   CSS files
        ...
build               
    ...
build.properties        contains the version number
build.xml               ant build file
lib                     Testing and non-eclipse dependencies as jars
    ...
misc                    SVG source images for the Marker icons
    ...
src                     source code
    log4j.properties    Log4J configuration
    ...                 
test                    test source code
    ...
                                    
```

The project uses Spring WebMVC and Spring Security. The latter is currently not really useful, but was left in to show how it can be easily done.

Users not being interested in doing web applications might just want to look at the spring
configs couchdb-config.xml, couchdb-services.xml and services-config.xml if they're interested in the non-web spring configuration. couchdb-services contains a spring configuration for the CouchDBUpdater that can sync view sources with CouchDB. See `/hood/WebContent/WEB-INF/designdocs` for an example of how to do it.

All other couchdb related code is is `/hood/src/org/hood/HoodServiceImpl.java`, `/hood/src/org/hood/LocationServiceImpl.java` and `/hood/src/org/hood/auth/AppUserDetailsService.java`

An important aspect of hood is the global JSON config. So far, the different parse path mappings required made this highly impractical, but with sub type mapping we can actually create a useful global JSONConfig as it is done by the class `org.hood.JSONConfigFactory` included in hood.

```
package org.hood;
import org.hood.domain.AppDocument;
import org.hood.domain.LatLongConverter;
import org.svenson.ClassNameBasedTypeMapper;
import org.svenson.JSON;
import org.svenson.JSONConfig;
import org.svenson.JSONParser;
import org.svenson.converter.DateConverter;
import org.svenson.converter.DefaultTypeConverterRepository;
import org.svenson.matcher.SubtypeMatcher;

/**
 * Creates a project global JSON config.
 * 
 * @author shelmberger
 *
 */
public class JSONConfigFactory
{
    public JSONConfig createJSONConfig()
    {
        JSONParser parser = new JSONParser();

        DefaultTypeConverterRepository typeConverterRepository = new DefaultTypeConverterRepository();
        typeConverterRepository.addTypeConverter(new DateConverter());
        typeConverterRepository.addTypeConverter(new LatLongConverter());
   
        // we use the new sub type matcher  
        ClassNameBasedTypeMapper typeMapper = new ClassNameBasedTypeMapper();
        typeMapper.setBasePackage(AppDocument.class.getPackage().getName());
        // we only want to have AppDocument instances
        typeMapper.setEnforcedBaseType(AppDocument.class);
        // we use the docType property of the AppDocument 
        typeMapper.setDiscriminatorField("docType");        
        // we only want to do the expensive look ahead if we're being told to
        // deliver AppDocument instances.        
        typeMapper.setPathMatcher(new SubtypeMatcher(AppDocument.class));

        parser.setTypeMapper(typeMapper);
        parser.setTypeConverterRepository(typeConverterRepository);

        JSON json = new JSON();
        json.setTypeConverterRepository(typeConverterRepository);

        return new JSONConfig(json, parser);
    }
}
```

The ClassNameBasedTypeMapper is using the new SubtypeMatcher that then triggers the decision process of the ClassNameBasedTypeMapper based on the previously mapped type hint.
In Hood's case this is our application-specific base type `org.hood.domain.AppDocument`

```
package org.hood.domain;

import org.hood.JSONConfigFactory;
import org.jcouchdb.document.BaseDocument;
import org.svenson.JSONProperty;

/**
 * Base class for the documents in our app. Contains a read-only documentType / docType property on
 * which the {@link JSONConfigFactory} used to discriminate types.
 * 
 * @author shelmberger
 *
 */
public class AppDocument extends BaseDocument
{
    /**
     * Returns the simple name of the class as doc type.
     * 
     * The annotation makes it a read-only property and also shortens the JSON name a little.
     * @return
     */
    @JSONProperty(value = "docType", readOnly = true)
    public String getDocumentType()
    {
        return this.getClass().getSimpleName();
    }
}

```

The code above shows AppDocument. It implements a read-only JSON-Property that automatically prints the current instance's simple name as "docType" property. Since this is readonly, it only influences the JSON generation of AppDocument instances.

On parsing documents with our globally configured JSONParser decide the actual type to parse into based on exactly the same read-only property. When using the global config, we now can just convert AppDocuments into JSON and can automatically convert that JSON back into the correct AppDocument derived type (which we also enforce).

Full circle configs are useful in situations where you have objects that you need to represent on both the java side as well as the client side or in CouchDB. For other scenarios it might make sense to only support one direction of conversion, or it might be useful to define a JSONifier or implement JSONable and let it generate a JSON structure that is a convenient transformation from the Java side. Larger applications might even require multiple JSON/Java circles etc. It's all your choice, your design.

The JSONConfigFactory also registers two type converters that can be used by annotating JSON Bean methods with @JSONConverter

```
package org.hood.domain;

import org.svenson.JSONProperty;
import org.svenson.converter.JSONConverter;

public class PositionedDocument
    extends AppDocument
{
    private LatLon location;

    private String name;

    private String description;

    /** {@inheritDoc} */
    public LatLon getLocation()
    {
        return location;
    }

    /** {@inheritDoc} */
    @JSONProperty("loc")
    @JSONConverter(type = LatLongConverter.class)
    public void setLocation(LatLon location)
    {
        this.location = location;
    }


    /** {@inheritDoc} */
    public String getName()
    {
        return name;
    }

    /** {@inheritDoc} */
    public void setName(String name)
    {
        this.name = name;
    }


    public String getDescription()
    {
        return description;
    }


    public void setDescription(String description)
    {
        this.description = description;
    }

}
```

Here we see PositionedDocument, the base class for all Hood objects with location. the @JSONProperty renames the LatLong typed property to "loc" and the @JSONConverter method effects the LatLong objects being converted into arrays and arrays into LatLong objects.