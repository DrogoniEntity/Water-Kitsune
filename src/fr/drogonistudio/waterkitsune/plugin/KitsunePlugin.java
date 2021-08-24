package fr.drogonistudio.waterkitsune.plugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Kitsune plugin informations.
 * 
 * <p>
 * This class will store any information about a plugin. Plugin's name,
 * description, version and initializer class name are stored.
 * </p>
 * 
 * <p>
 * Initializer class name must contains following method :
 * {@code public static void initialize(KitsunePlugin)}. This method is called
 * during plugin initialization process. If plugin doesn't have a initializer
 * class, then none method is called during initialization process.
 * </p>
 * 
 * @author DrogoniEntity
 */
public final class KitsunePlugin
{
    
    /**
     * Default description.
     */
    public static final String NO_DESCRIPTION = "(No description)";
    
    /**
     * Default version.
     */
    public static final String DEFAULT_VERSION = "1.0";
    
    /**
     * Plugin's name.
     * 
     * <p>
     * By default, it will be plugin's file name. This name cannot be null.
     * </p>
     */
    private final String name;
    
    /**
     * Plugin's description.
     */
    private final String description;
    
    /**
     * Plugin's version.
     */
    private final String version;
    
    /**
     * Plugin's initializer class name.
     * 
     * <p>
     * This class is loaded during initialization process and invoke
     * {@code public static void initialize(KitsunePlugin)} with this plugin as
     * parameter.
     * </p>
     */
    protected final String initializerClassName;
    
    /**
     * Setup informations.
     * 
     * <p>
     * Any of this value are must be non-null except to {@code initializerClass}. If
     * description and/or version are null, default value are applied.
     * </p>
     * 
     * @param name
     *            - Plugin's name.
     * @param description
     *            - Plugin's description.
     * @param version
     *            - Plugin's version.
     * @param initializerClass
     *            - Initializer class name.
     * @throws NullPointerException
     *             if name is null.
     */
    protected KitsunePlugin(String name, String description, String version, String initializerClass)
	    throws IllegalArgumentException
    {
	if (name == null)
	    throw new NullPointerException("Name cannot be null");
	this.name = name;
	
	if (description == null)
	    description = NO_DESCRIPTION;
	this.description = description;
	
	if (version == null)
	    version = DEFAULT_VERSION;
	this.version = version;
	
	this.initializerClassName = initializerClass;
    }
    
    /**
     * Setup information with default value.
     * 
     * @param name
     *            - Plugin's name.
     * @throws NullPointerException
     *             if name is null.
     */
    protected KitsunePlugin(String name) throws NullPointerException
    {
	this(name, NO_DESCRIPTION, DEFAULT_VERSION, null);
    }
    
    /**
     * Parse meta file from {@code file}.
     * 
     * <p>
     * Meta file are JSON file which contains the following JSON object:
     * 
     * <pre>
     * {
     * 	"name": &lt;plugin name&gt;,
     * 	"description": &lt;plugin description&gt;,
     * 	"version": &lt;plugin version&gt;,
     * 	"initializer": &lt;initializer class name&gt;
     * }
     * </pre>
     * </p>
     * 
     * @param file
     *            - meta file
     * @return read plugin.
     * 	   
     * @throws IOException
     *             if an I/O exception occurred during file reading
     * @see #parseFromStream(InputStream) {@code parseFromStream(InputStream)} to
     *      parse from an generic stream.
     */
    public static KitsunePlugin parseFromFile(File file) throws IOException
    {
	return parse(new FileReader(file));
    }
    
    /**
     * Parse meta file from {@code stream}.
     * 
     * <p>
     * Meta file are JSON file which contains the following JSON object:
     * 
     * <pre>
     * {
     * 	"name": &lt;plugin name&gt;,
     * 	"description": &lt;plugin description&gt;,
     * 	"version": &lt;plugin version&gt;,
     * 	"initializer": &lt;initializer class name&gt;
     * }
     * </pre>
     * </p>
     * 
     * <p>
     * {@code stream} must point to a JSON file.
     * </p>
     * 
     * @param stream
     *            - meta file's stream
     * @return read plugin.
     * @throws IOException
     *             if an I/O exception occurred during stream reading
     * @see #parseFromFile(File) {@code parseFromStream(InputStream)} to parse from
     *      a file.
     */
    public static KitsunePlugin parseFromStream(InputStream stream) throws IOException
    {
	return parse(new InputStreamReader(stream));
    }
    
    /**
     * Generic parse method.
     * 
     * <p>
     * This method is used to perform parse job.
     * </p>
     * 
     * @param reader
     *            - reader who point to meta file.
     * @return read plugin.
     * @see #parseFromFile(File)
     * @see #parseFromStream(InputStream)
     */
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
	    version = object.get("version").getAsString();
	
	String initializer = null;
	if (object.has("initializer"))
	    initializer = object.get("initializer").getAsString();
	
	return new KitsunePlugin(name, description, version, initializer);
    }
    
    /**
     * Getting plugin's name.
     * 
     * @return plugin's name.
     */
    public final String getName()
    {
	return this.name;
    }
    
    /**
     * Getting plugin's description.
     * 
     * @return plugin's description.
     */
    public final String getDescription()
    {
	return this.description;
    }
    
    /**
     * Getting plugin's version name.
     * 
     * @return plugin's version name.
     */
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
