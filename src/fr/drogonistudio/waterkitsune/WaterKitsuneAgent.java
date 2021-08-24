package fr.drogonistudio.waterkitsune;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import fr.drogonistudio.waterkitsune.plugin.KitsunePlugin;
import fr.drogonistudio.waterkitsune.plugin.KitsunePluginManager;

/**
 * Agent program.
 * 
 * @author DrogoniEntity
 */
public class WaterKitsuneAgent
{
    /**
     * Premain method.
     * 
     * <p>
     * It will simply invoke {@code agentstart()}. No extra job is made.
     * </p>
     * 
     * @param agentArgs - Argument used when launching agent (unused).
     * @param instr - Instrumentation used during JVM's life-cycle.
     */
    public static void premain(String agentArgs, Instrumentation instr)
    {
	agentstart(agentArgs, instr);
    }
    
    /**
     * Initialize process.
     * 
     * <p>
     * It will load plugins and register custom transformer to {@code instr}.
     * If something went wrong during initialization, transformer will not be applied. 
     * </p>
     * 
     * @param agentArgs - Argument used when launching agent (unused).
     * @param instr - Instrumentation used during JVM's life-cycle. 
     */
    public static void agentstart(String agentArgs, Instrumentation instr)
    {
	WaterKitsuneLogger.info("(Agent ver. 1.0.2)");
	
	KitsunePluginManager manager = new KitsunePluginManager(agentArgs);
	
	try
	{
	    if (Boolean.parseBoolean(System.getProperty("waterkitsune.openexportall", "false")))
	    {
		WaterKitsuneLogger.info("Open and export modules...");
		OpenExporterModulesHack.openExport(instr);
	    }
	    
	    // Initialize plugins...
	    WaterKitsuneLogger.info("Setting up plugins...");
	    manager.readPlugins();
	    manager.loadPlugins(instr);
	    
	    // With loaded plugins, we will setup transformer...
	    List<KitsunePlugin> plugins = manager.getPlugins();
	    List<File> pluginsFiles = new ArrayList<>(plugins.size());
	    for (KitsunePlugin plugin : plugins)
		pluginsFiles.add(manager.getPluginFile(plugin));
	    
	    WaterKitsuneLogger.info("Adding transformer...");
	    instr.addTransformer(new KitsuneTransformer(pluginsFiles));
	    
	    WaterKitsuneLogger.info("(Agent loaded)");
	}
	catch (Throwable t)
	{
	    WaterKitsuneLogger.error("Fatal error occured during agent initialization !");
	    t.printStackTrace();
	    WaterKitsuneLogger.error("Class transformation couldn't be applied.");
	}
    }
    
    /**
     * Plugin transformer.
     * 
     * <p>
     * With loaded plugins, it will transform classes by loading alternative classes in plugins' file.
     * </p>
     * 
     * <p>
     * Only, classes when their name start with {@code java. } or {@code fr.drogonistudio.waterkitsune. } are ignored
     * by it.
     * </p>
     * 
     * @author DrogoniEntity
     */
    private static class KitsuneTransformer implements ClassFileTransformer
    {
	/**
	 * Buffer size used when we load classes from files.
	 */
	private static final int BUFFER_SIZE = 4096;
	
	/**
	 * Files to read when we need to transform classes.
	 */
	private List<File> files;
	
	public KitsuneTransformer(List<File> files)
	{
	    this.files = Collections.unmodifiableList(files);
	}
	
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
		ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException
	{
	    if (!className.startsWith("java.") && !className.startsWith("fr.drogonistudio.waterkitsune."))
	    {
		byte[] newBuffer = this.getTransformedClass(className);
		if (newBuffer != null)
		{
		    WaterKitsuneLogger.debug("Transform class \"%s\"", className);
		    return newBuffer;
		}
	    }
	    return classfileBuffer;
	}
	
	/**
	 * Reading new class data from plugins files.
	 * 
	 * <p>
	 * It will load class file data from ZIP file for a class file. It will load
	 * only the first class found.
	 * </p>
	 * <p>
	 * If nothing is found, {@code null} is returned.
	 * </p>
	 * 
	 * @param className - Class to load.
	 * @return new class data or {@code null} if plugins doesn't have custom data for {@code className}.
	 */
	private byte[] getTransformedClass(String className)
	{
	    String resource = className.replace('.', '/').concat(".class");
	    
	    for (int i = 0; i < this.files.size(); i++)
	    {
		File container = this.files.get(i);
		if (container.isDirectory())
		{
		    File resourceFile = new File(container, resource);
		    if (resourceFile.exists())
		    {
			try
			{
			    FileInputStream stream = new FileInputStream(resourceFile);
			    byte data[] = readFully(stream);
			    stream.close();
			    
			    return data;
			}
			catch (IOException ioEx)
			{
			    // Ignore it
			}
		    }
		}
		else
		{
		    ZipFile zipFile = null;
		    try
		    {
			zipFile = new ZipFile(container);
			ZipEntry entry = zipFile.getEntry(resource);
			
			if (entry != null)
			{
			    InputStream stream = zipFile.getInputStream(entry);
			    byte[] data = readFully(stream);
			    stream.close();
			    
			    return data;
			}
		    }
		    catch (IOException ioEx)
		    {
			// Ignored
		    }
		    finally
		    {
			if (zipFile != null)
			{
			    try
			    {
				zipFile.close();
			    }
			    catch (IOException ioEx)
			    {
				// Ignored
			    }
			}
		    }
		}
	    }
	    
	    return null;
	}
	
	/**
	 * Utility to read a stream.
	 * 
	 * <p>
	 * It will read each bytes from {@code stream}. If something when wrong during
	 * file reading, an exception is thrown.
	 * </p>
	 * 
	 * @param stream - File's stream.
	 * @return read bytes.
	 * @throws IOException - if something went wring during file reading.
	 */
	private static byte[] readFully(InputStream stream) throws IOException
	{
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    byte buffer[] = new byte[BUFFER_SIZE];
	    int readed;
	    
	    while ((readed = stream.read(buffer)) > 0)
		baos.write(buffer, 0, readed);
	    
	    return baos.toByteArray();
	}
    }
}
