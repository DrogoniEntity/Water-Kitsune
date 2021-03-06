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
package fr.drogonistudio.waterkitsune;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import fr.drogonistudio.waterkitsune.transformation.LightKitsuneTransformManager;

/**
 * Plugin transformer.
 * 
 * <p>
 * With loaded plugins, it will transform classes by loading alternative classes
 * in plugins' file.
 * </p>
 * 
 * <p>
 * Only, classes when their name start with {@code java. } or
 * {@code fr.drogonistudio.waterkitsune. } are ignored by it.
 * </p>
 * 
 * @author DrogoniEntity
 */
class KitsuneTransformer implements ClassFileTransformer
{
    
    private static final String AGENT_PACKAGE = WaterKitsuneAgent.class.getPackageName().replace('.', '/');
    
    /**
     * List of all excluded packages.
     * 
     * <p>
     * Classes from these packages should not be transformed.
     * </p>
     */
    private static final String[] PACKAGES_EXCLUDED = new String[] {
	    "java/",
	    "sun/",
	    "javax/",
	    AGENT_PACKAGE
    };
    
    /**
     * Buffer size used when we load classes from files.
     */
    private static final int BUFFER_SIZE = 8192;
    
    /**
     * Files to read when we need to transform classes.
     */
    private List<File> files;
    
    /**
     * Opened Zip files.
     */
    private Map<File, ZipFile> zipFiles;
    
    /**
     * Light transformer manager reference.
     */
    private final LightKitsuneTransformManager lightTransformers;
    
    public KitsuneTransformer(List<File> files, LightKitsuneTransformManager lightTransformers)
    {
	this.files = files;
	this.zipFiles = new HashMap<>();
	
	for (File f : this.files)
	{
	    if (f.isFile())
	    {
		try
		{
		    ZipFile zf = new ZipFile(f);
		    this.zipFiles.put(f, zf);
		} catch (IOException ioEx)
		{
		    // Ignore it
		}
	    }
	}
	Runtime.getRuntime().addShutdownHook(new Thread(() -> this.zipFiles.forEach((file, zip) ->
	{
	    // @formatter:off
    	    try { zip.close(); }
    	    catch (IOException ignored) {}
    	    // @formatter:on
	}), "WaterKitsune-Shutdown"));
	    
	this.lightTransformers = lightTransformers;
    }
    
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
	    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException
    {
	if (!isExculuded(className))
	{
	    WaterKitsuneLogger.debug(Level.FINE, "Looking at \"%s\" class", className);
	    
	    // Flag to notify it has been changed
	    boolean changed = false;
	    
	    // Reading from files
	    byte[] newBuffer = this.getHardTransformedClass(className);
	    if (newBuffer != null)
		changed = true;
	    else
		// No change, reset newBuffer to current data
		newBuffer = classfileBuffer;
	    
	    // Applying light transformers
	    newBuffer = this.lightTransformers.apply(className, newBuffer);
	    if (!changed && !Arrays.equals(classfileBuffer, newBuffer))
		changed = true;
	    
	    // Log debug message to indicate we have transformed 'className'
	    if (changed)
	    {
		WaterKitsuneLogger.debug(Level.FINE, "Class \"%s\" transformed", className);
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
     * @param className
     *            - Class to load.
     * @return new class data or {@code null} if plugins doesn't have custom data
     *         for {@code className}.
     */
    private byte[] getHardTransformedClass(String className)
    {
	String resource = className.replace('.', '/').concat(".class");
	
	for (int i = 0; i < this.files.size(); i++)
	{
	    File container = this.files.get(i);
	    if (this.zipFiles.containsKey(container))
	    {
		try
		{
		    ZipFile file = this.zipFiles.get(container);
		    ZipEntry entry = file.getEntry(resource);
		    
		    if (entry != null)
		    {
			InputStream stream = file.getInputStream(entry);
			byte[] data = readFully(stream);
			stream.close();
			
			return data;
		    }
		} catch (IOException ioEx)
		{
		    WaterKitsuneLogger.debug(Level.SEVERE, "Failed to read class \"%s\" from Zip file \"%s\" (%s)",
			    className, container.getName(), ioEx.getMessage());
		}
	    } else if (container.isDirectory())
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
		    } catch (IOException ioEx)
		    {
			// Ignore it
			WaterKitsuneLogger.debug(Level.SEVERE, "Failed to read class \"%s\" from Zip file \"%s\" (%s)",
				className, container.getName(), ioEx.getMessage());
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
     * @param stream
     *            - File's stream.
     * @return read bytes.
     * @throws IOException
     *             - if something went wring during file reading.
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
    
    void updateOpenedFilesList(List<File> filesToKeep)
    {
	for (File file : filesToKeep)
	{
	    if (!this.files.contains(file))
	    {
		this.files.remove(file);
		ZipFile zipFile = this.zipFiles.get(file);
		if (zipFile != null)
		{
		// @formatter:off
    		try { zipFile.close(); }
    		catch (IOException ignored) {}
    		// @formatter:on
		}
	    }
	}
    }
    
    void freezeFilesList()
    {
	this.files = Collections.unmodifiableList(this.files);
	this.zipFiles = Collections.unmodifiableMap(this.zipFiles);
    }
    
    /**
     * Check if {@className} is located inside a excluded package.
     * 
     * <p>
     * If it was the case, this class may not be transformed.
     * </p>
     * 
     * @param className - class's name to check
     * @return {@code true} if class is located inside a excluded packages.
     * @see #PACKAGES_EXCLUDED
     */
    private static boolean isExculuded(String className)
    {
	for (int i = 0; i < PACKAGES_EXCLUDED.length; i++)
	{
	    if (className.startsWith(PACKAGES_EXCLUDED[i]))
		return true;
	}
	
	return false;
    }
}