package fr.drogonistudio.waterkitsune.plugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class KitsunePlugin
{
    public static final String NO_DESCRIPTION = "(No description)";
    public static final String DEFAULT_VERSION = "1.0";
    
    private final String name;
    private final String description;
    
    private final String version;
    protected final String initializerClassName;
    
    protected KitsunePlugin(String name, String description, String version, String initializerClass)
    {
	this.name = name;
	this.description = description;
	this.version = version;
	this.initializerClassName = initializerClass;
    }
    
    protected KitsunePlugin(String name)
    {
	this(name, NO_DESCRIPTION, DEFAULT_VERSION, null);
    }
    
    public static KitsunePlugin parseFromFile(File file) throws IOException
    {
	return parse(new FileReader(file));
    }
    
    public static KitsunePlugin parseFromStream(InputStream stream) throws IOException
    {
	return parse(new InputStreamReader(stream));
    }
    
    private static KitsunePlugin parse(Reader reader)
    {
	JsonParser parser = new JsonParser();
	JsonObject object = parser.parse(reader).getAsJsonObject();
	
	String name = object.get("name").getAsString();
	
	String description = NO_DESCRIPTION;
	if (object.has("description"))
	    description = object.get("description").getAsString();
	
	String version = object.get("version").getAsString();
	if (object.has("version"))
	    description = object.get("version").getAsString();
	
	String initializer = null;
	if (object.has("initializer"))
	    initializer = object.get("initializer").getAsString();
	
	return new KitsunePlugin(name, description, version, initializer);
    }

    public final String getName()
    {
        return this.name;
    }
    
    public final String getDescription()
    {
        return this.description;
    }
    
    public final String getVersion()
    {
        return this.version;
    }
    
    @Override
    public String toString()
    {
	return this.name + " (ver. " + this.version + ")";
    }

    @Override
    public int hashCode()
    {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((description == null) ? 0 : description.hashCode());
	result = prime * result + ((initializerClassName == null) ? 0 : initializerClassName.hashCode());
	result = prime * result + ((name == null) ? 0 : name.hashCode());
	result = prime * result + ((version == null) ? 0 : version.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj)
    {
	if (this == obj)
	    return true;
	if (!(obj instanceof KitsunePlugin))
	    return false;
	KitsunePlugin other = (KitsunePlugin) obj;
	if (description == null)
	{
	    if (other.description != null)
		return false;
	} else if (!description.equals(other.description))
	    return false;
	if (this.initializerClassName == null)
	{
	    if (other.initializerClassName != null)
		return false;
	} else if (!initializerClassName.equals(other.initializerClassName))
	    return false;
	if (this.name == null)
	{
	    if (other.name != null)
		return false;
	} else if (!name.equals(other.name))
	    return false;
	if (this.version == null)
	{
	    if (other.version != null)
		return false;
	} else if (!version.equals(other.version))
	    return false;
	return true;
    }
}
