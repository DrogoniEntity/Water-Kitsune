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

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

import fr.drogonistudio.waterkitsune.plugin.KitsunePlugin;
import fr.drogonistudio.waterkitsune.plugin.KitsunePluginManager;
import fr.drogonistudio.waterkitsune.transformation.LightKitsuneTransformManager;

/**
 * Agent program.
 * 
 * @author DrogoniEntity
 */
public final class WaterKitsuneAgent
{
    private static final String VERSION = "1.2";
    
    /**
     * Premain method.
     * 
     * <p>
     * It will simply invoke {@code agentstart()}. No extra job is made.
     * </p>
     * 
     * @param agentArgs
     *            - Argument used when launching agent (unused).
     * @param instr
     *            - Instrumentation used during JVM's life-cycle.
     */
    public static void premain(String agentArgs, Instrumentation instr)
    {
	agentstart(agentArgs, instr);
    }
    
    /**
     * Initialize process.
     * 
     * <p>
     * It will load plugins and register custom transformer to {@code instr}. If
     * something went wrong during initialization, transformer will not be applied.
     * </p>
     * 
     * @param agentArgs
     *            - Argument used when launching agent (unused).
     * @param instr
     *            - Instrumentation used during JVM's life-cycle.
     */
    public static void agentstart(String agentArgs, Instrumentation instr)
    {
	WaterKitsuneLogger.info("(Agent ver. %s)", VERSION);
	
	final KitsunePluginManager pluginManager = new KitsunePluginManager(agentArgs);
	final LightKitsuneTransformManager lightTransforms = new LightKitsuneTransformManager();
	
	try
	{
	    if (Boolean.parseBoolean(System.getProperty("waterkitsune.openexportall", "false")))
	    {
		WaterKitsuneLogger.info("Open and export modules...");
		OpenExporterModulesHack.openExport(instr);
	    }
	    
	    // Reading installed plugins
	    WaterKitsuneLogger.info("Setting up plugins...");
	    pluginManager.readPlugins();
	    
	    // Setup transformer to allow transforming earlier as possible
	    WaterKitsuneLogger.info("Adding transformer...");
	    KitsuneTransformer transformer = new KitsuneTransformer(pluginsFilesList(pluginManager), lightTransforms);
	    instr.addTransformer(transformer, instr.isRetransformClassesSupported());
	    
	    // Now initialize plugins
	    pluginManager.loadPlugins(instr);
	    lightTransforms.lockRegistrations();
	    
	    // Now, update opened files list since some plugins have been disabled
	    WaterKitsuneLogger.info("Updating plugins list...");
	    transformer.updateOpenedFilesList(pluginsFilesList(pluginManager));
	    transformer.freezeFilesList();
	    
	    // Now, we are ready !
	    WaterKitsuneLogger.info("(Agent loaded)");
	} catch (Throwable t)
	{
	    WaterKitsuneLogger.thrown("Fatal error occured during agent initialization !", t);
	    WaterKitsuneLogger.error("Class transformation couldn't be applied.");
	}
    }
    
    private static List<File> pluginsFilesList(KitsunePluginManager manager)
    {
	List<KitsunePlugin> plugins = manager.getPlugins();
	List<File> files = new ArrayList<>(plugins.size());
	
	for (KitsunePlugin plugin : plugins)
	    files.add(manager.getPluginFile(plugin));
	
	return files;
    }
    
    public static String getVersion()
    {
	return VERSION;
    }
}
