/*
 * Water-Kitsune, a simple patch loader
 * Copyright (C) 2021  Drogoni-Studio

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package fr.drogonistudio.waterkitsune.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import fr.drogonistudio.waterkitsune.WaterKitsuneLogger;

/**
 * Plugin manager class.
 * 
 * <p>
 * Plugin manager will store any plugin information and which files plugins
 * point to and manage them to load, run initializer, etc.
 * </p>
 * 
 * <p>
 * Plugins are retrieved from a list file (commonly named as ".enabled"). This
 * file will indicate which plugins to load from relative path from this file,
 * or absolute path. During file reading, plugins informations are retrieved.
 * After that, they are loaded and now, they are ready.
 * </p>
 * 
 * @author DrogoniEntity
 */
public class KitsunePluginManager
{
    
    /**
     * Default plugin list file.
     */
    private static final String DEFAULT_PLUGIN_LIST_FILE = "kitsune-plugins/.enabled";
    
    /**
     * Manager instance.
     * 
     * <p>
     * A plugin manager can only created one time.
     * </p>
     */
    private static KitsunePluginManager instance;
    
    /**
     * Path of list file.
     * 
     * <p>
     * This list can be a directory or a text file. If it was a text file, only
     * files who are present in this file are loaded. However, every file from this
     * directory are loaded.
     * </p>
     */
    private final String pluginsListFile;
    
    /**
     * List of all active plugins.
     * 
     * <p>
     * This list is accessible only by one thread.
     * </p>
     */
    private List<Entry<File, KitsunePlugin>> plugins;
    
    /**
     * List of plugins who request to be unloaded.
     */
    private List<KitsunePlugin> pluginsToStopLoading;
    
    /**
     * Current loading statement.
     * 
     * <p>
     * There is 4 statement :
     * </p>
     * <ol>
     * <li>File not reading</li>
     * <li>File reading</li>
     * <li>Plugins are loading</li>
     * <li>Plugins are loaded</li>
     * </ol>
     * 
     * <p>
     * By default, the loading statement is set to "file not reading".
     * </p>
     */
    private PluginLoadingStatement loadingStatement;
    
    /**
     * Current loading plugin.
     */
    private KitsunePlugin currentlyToLoad;
    
    /**
     * Initialize plugin manager.
     * 
     * <p>
     * It will only setup lists and constants. Plugins are not loaded. If {@code pluginListFile}
     * is null or empty, the default value is used.
     * </p>
     * 
     * <p>
     * Plugin manager can be created only one time and this constructor can only be
     * invoked one time.
     * </p>
     * 
     * @param pluginListFile
     *            - plugin list file.
     * @throws IllegalStateException
     *             when this constructor has already been invoked.
     * @see #readPlugins() - read plugins informations
     */
    public KitsunePluginManager(String pluginListFile) throws IllegalStateException
    {
	if (instance != null)
	    throw new IllegalStateException("Plugin manager already loaded");
	
	instance = this;
	
	if (pluginListFile != null && !pluginListFile.isEmpty())
	    this.pluginsListFile = pluginListFile;
	else
	    this.pluginsListFile = DEFAULT_PLUGIN_LIST_FILE;
	
	this.plugins = Collections.synchronizedList(new ArrayList<>());
	this.loadingStatement = PluginLoadingStatement.FILE_NOT_READED;
    }
    
    /**
     * Reading list file and loading plugins.
     * 
     * <p>
     * It will clear plugins list and begin to read plugins files depends
     * on how their are stored (if plugins are zip file or directory). It will
     * </p>
     * 
     * @throws IOException if an I/O error occurred.
     * @throws IllegalStateException if plugins already readed.
     */
    public void readPlugins() throws IOException, IllegalStateException
    {
	if (this.loadingStatement != PluginLoadingStatement.FILE_NOT_READED)
	    throw new IllegalStateException("Plugins already readed !");
	
	this.plugins.clear();
	
	File pluginsListFile = new File(this.pluginsListFile);
	
	List<Entry<File, KitsunePlugin>> nextList;
	if (pluginsListFile.exists())
	{
	    if (pluginsListFile.isDirectory())
		nextList = this.readPluginsFromDirectory(pluginsListFile);
	    else
		nextList = this.readPluginsFromFile(pluginsListFile);
	}
	else
	{
	    boolean hasFoundValidDirectory;
	    File directoryToUse = pluginsListFile;
	    do
	    {
		directoryToUse = directoryToUse.getParentFile();
		hasFoundValidDirectory = directoryToUse.exists();
	    } while (hasFoundValidDirectory);
	    
	    nextList = this.readPluginsFromDirectory(directoryToUse);
	}
	
	this.plugins.addAll(nextList);
	this.loadingStatement = PluginLoadingStatement.FILE_READED;
    }
    
    private List<Entry<File, KitsunePlugin>> readPluginsFromFile(File file) throws IOException
    {
	File parentDirectory = file.getParentFile();
	List<File> pluginsFiles = new ArrayList<>();
	
	BufferedReader reader = null;
	try
	{
	    reader = new BufferedReader(new FileReader(file));
	    String line;
	    
	    while ((line = reader.readLine()) != null)
	    {
		if (!line.startsWith("#"))
		{
		    File pluginFile;
		    Path filepath = Paths.get(line);
		    if (filepath.isAbsolute())
			pluginFile = new File(line);
		    else
			pluginFile = new File(parentDirectory, line);
		    
		    if (pluginFile.exists())
			pluginsFiles.add(pluginFile);
		    else
			WaterKitsuneLogger.error("Plugin file \"%s\" not found", pluginFile.getName());
		}
	    }
	} finally
	{
	    if (reader != null)
		reader.close();
	}
	
	List<Entry<File, KitsunePlugin>> entries = new ArrayList<>();
	for (File pluginFile : pluginsFiles)
	{
	    try
	    {
		KitsunePlugin readed = parsePluginFile(pluginFile);
		entries.add(new SimpleEntry<>(pluginFile, readed));
	    } catch (IOException ioEx)
	    {
		WaterKitsuneLogger.error("Couldn't parse plugin file \"%s\" (%s)", pluginFile.getName(),
			ioEx.getMessage());
	    }
	}
	
	return entries;
    }
    
    private List<Entry<File, KitsunePlugin>> readPluginsFromDirectory(File directory)
    {
	List<Entry<File, KitsunePlugin>> plugins = new ArrayList<>();
	
	if (!directory.exists())
	    return plugins;
	
	File files[] = directory.listFiles();
	for (int i = 0; i < files.length; i++)
	{
	    File toRead = files[i];
	    
	    if (toRead.isDirectory())
	    {
		try
		{
		    KitsunePlugin readed = parsePluginFile(toRead);
		    plugins.add(new SimpleEntry<>(toRead, readed));
		} catch (IOException ioEx)
		{
		    WaterKitsuneLogger.error("Couldn't parse plugin file \"%s\" (%s)", toRead.getName(),
			    ioEx.getMessage());
		}
	    } else
	    {
		try
		{
		    ZipFile isZip = new ZipFile(toRead);
		    isZip.close();
		    
		    try
		    {
			KitsunePlugin readed = parsePluginFile(toRead);
			plugins.add(new SimpleEntry<>(toRead, readed));
		    } catch (IOException ioEx)
		    {
			WaterKitsuneLogger.error("Couldn't parse plugin file \"%s\" (%s)", toRead.getName(),
				ioEx.getMessage());
		    }
		} catch (IOException ignored)
		{
		    // It's not a valid file
		}
	    }
	}
	
	return plugins;
    }
    
    private static KitsunePlugin parsePluginFile(File pluginFile) throws IOException
    {
	if (pluginFile.isDirectory())
	{
	    File metaFile = new File(pluginFile, "plugin.kitmeta");
	    if (metaFile.exists())
		return KitsunePlugin.parseFromFile(metaFile);
	} else
	{
	    ZipFile pluginZip = new ZipFile(pluginFile);
	    try
	    {
		ZipEntry metaEntry = pluginZip.getEntry("plugin.kitmeta");
		
		if (metaEntry != null)
		    return KitsunePlugin.parseFromStream(pluginZip.getInputStream(metaEntry));
	    } finally
	    {
		pluginZip.close();
	    }
	    
	}
	
	WaterKitsuneLogger.warning("Plugin \"%s\" has no meta file", pluginFile.getName());
	return new KitsunePlugin(pluginFile.getName());
    }
    
    public void loadPlugins(Instrumentation instr) throws Exception
    {
	switch (this.loadingStatement)
	{
	    case FILE_NOT_READED:
		throw new IllegalStateException("Plugin manager is not ready to load plugins");
	    case LOADING:
		throw new IllegalStateException("Loading process already started");
	    case LOADED:
		throw new IllegalStateException("Plugins already loaded");
	    case FILE_READED:
		this.loadingStatement = PluginLoadingStatement.LOADING;
		
		this.pluginsToStopLoading = new LinkedList<>();
		
		Iterator<Entry<File, KitsunePlugin>> pluginsIterator = this.plugins.iterator();
		final String classpathSeparator = System.getProperty("path.separator");
		
		while (pluginsIterator.hasNext())
		{
		    Entry<File, KitsunePlugin> entry = pluginsIterator.next();
		    this.currentlyToLoad = entry.getValue();
		    
		    File pluginFile = entry.getKey();
		    if (pluginFile.isDirectory())
		    {
			pluginFile = packFile(pluginFile).toFile();
			pluginFile.deleteOnExit();
		    }
		    
		    instr.appendToSystemClassLoaderSearch(new JarFile(pluginFile));
		    System.setProperty("java.class.path", System.getProperty("java.class.path") + classpathSeparator
			    + entry.getKey().getAbsolutePath());
		    try
		    {
			if (this.currentlyToLoad.initializerClassName != null)
			{
			    WaterKitsuneLogger.info("Initialize \"%s\" (version %s)...", this.currentlyToLoad.getName(),
				    this.currentlyToLoad.getVersion());
			    Class<?> initializerClass = Class.forName(this.currentlyToLoad.initializerClassName);
			    Method initMethod = initializerClass.getMethod("initialize", KitsunePlugin.class);
			    initMethod.invoke(null, this.currentlyToLoad);
			    WaterKitsuneLogger.info("\"%s\" initialized.", this.currentlyToLoad.getName());
			}
			
		    } catch (Throwable fatal)
		    {
			WaterKitsuneLogger.error("Fatal error occured during \"%s\" initialization.", entry.getValue());
			fatal.printStackTrace();
		    }
		}
		
		this.currentlyToLoad = null;
		
		// Unload all requested plugins want to stop
		for (KitsunePlugin toStop : this.pluginsToStopLoading)
		{
		    for (int i = 0; i < this.plugins.size(); i++)
		    {
			Entry<File, KitsunePlugin> entry = this.plugins.get(i);
			if (entry.getValue() == toStop)
			{
			    this.plugins.remove(i);
			    return;
			}
		    }
		}
		
		// Free memory about stopping process...
		this.pluginsToStopLoading.clear();
		this.pluginsToStopLoading = null;
		
		// Now, make list and loaders to not be able to be modified
		this.plugins = Collections.unmodifiableList(this.plugins);
		
		this.loadingStatement = PluginLoadingStatement.LOADED;
		break;
	}
    }
    
    public void stopLoading(KitsunePlugin toStop)
    {
	if (this.loadingStatement != PluginLoadingStatement.LOADING)
	    throw new IllegalStateException("Couldn't stop plugin at this time");
	if (this.currentlyToLoad != toStop)
	    throw new IllegalStateException("Couldn't stop another plugin");
	WaterKitsuneLogger.info("Stopping to load \"%s\"...", toStop.getName());
	
	this.pluginsToStopLoading.add(toStop);
    }
    
    public KitsunePlugin getPlugin(String name)
    {
	for (int i = 0; i < this.plugins.size(); i++)
	{
	    KitsunePlugin plugin = this.plugins.get(i).getValue();
	    if (plugin.getName().equals(name))
		return plugin;
	}
	
	return null;
    }
    
    public List<KitsunePlugin> getPlugins()
    {
	List<KitsunePlugin> out = new ArrayList<>();
	for (int i = 0; i < this.plugins.size(); i++)
	    out.add(this.plugins.get(i).getValue());
	
	return Collections.unmodifiableList(out);
    }
    
    public File getPluginFile(KitsunePlugin plugin)
    {
	for (int i = 0; i < this.plugins.size(); i++)
	{
	    Entry<File, KitsunePlugin> entry = this.plugins.get(i);
	    if (entry.getValue() == plugin)
		return entry.getKey();
	}
	
	return null;
    }
    
    public PluginLoadingStatement getLoadingStatement()
    {
	return this.loadingStatement;
    }
    
    public static final KitsunePluginManager getManager()
    {
	return instance;
    }
    
    private static Path packFile(File directory) throws IOException
    {
	WaterKitsuneLogger.debug(Level.FINE, "Packaging \"%s\"...", directory.getPath());
	Path tmpPath = Files.createTempFile("waterkitsune-", ".zip");
	try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(tmpPath)))
	{
	    Path source = directory.toPath();
	    Files.walk(source).filter((path) -> !Files.isDirectory(path)).forEach((path) ->
	    {
		try
		{
		    ZipEntry entry = new ZipEntry(source.relativize(path).toString());
		    zipOut.putNextEntry(entry);
		    Files.copy(path, zipOut);
		    zipOut.closeEntry();
		} catch (IOException ioEx)
		{
		    WaterKitsuneLogger.error("Couldn't put entry \"%s\" into \"%s\": %s", path.toString(),
			    tmpPath.toString(), ioEx.getMessage());
		}
	    });
	}
	
	return tmpPath;
    }
    
    public static enum PluginLoadingStatement
    {
	FILE_NOT_READED, FILE_READED, LOADING, LOADED;
    }
}
