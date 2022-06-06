package fr.drogonistudio.waterkitsune.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import fr.drogonistudio.waterkitsune.WaterKitsuneLogger;

public class LoadingSimulation
{
    private final File loadingInfoFile;
    
    private File classesLocation;
    private KitsunePlugin plugin;
    private LoadingPriority loadingPriority;
    
    public LoadingSimulation(File loadingInfoFile) throws NullPointerException, FileNotFoundException
    {
	Objects.requireNonNull(loadingInfoFile, "Loading information file not specified");
	if (!loadingInfoFile.exists())
	    throw new FileNotFoundException(loadingInfoFile.getName() + " not found");
	
	this.loadingInfoFile = loadingInfoFile;
    }
    
    public void load() throws Throwable
    {
	Map<String, File> files = this.loadSimulationInfo();
	if (!files.get("classes").exists())
	    throw new FileNotFoundException("Classes location not found");
	if (!files.get("info").exists())
	    throw new FileNotFoundException("Information file not found");
	KitsunePlugin plugin = KitsunePlugin.parseFromFile(files.get("info"));
	
	this.classesLocation = files.get("classes");
	this.plugin = plugin;
    }
    
    public void initializePlugin()
    {
	if (this.plugin.initializerClassName != null)
	{
	    WaterKitsuneLogger.info("[SIMULATION] Initialize \"%s\"...", this.plugin.toString());
	    
	    try
	    {
		Class<?> initializerClass = Class.forName(this.plugin.initializerClassName);
		initializerClass.getMethod("initialize", KitsunePlugin.class).invoke(null, this.plugin);
	    } catch (Throwable fatal)
	    {
		WaterKitsuneLogger
			.thrown(String.format("[SIMULATION] Fatal error occured during \"%s\" initialization.",
				this.plugin.toString()), fatal);
	    }
	    
	    WaterKitsuneLogger.info("[SIMULATION] \"%s\" initialized.", this.plugin.getName());
	}
    }
    
    private Map<String, File> loadSimulationInfo() throws IOException, NullPointerException
    {
	FileReader reader = new FileReader(this.loadingInfoFile);
	JsonObject simulationInfo = Json.parse(reader).asObject();
	
	if (simulationInfo.get("classes-location") == null)
	    throw new NullPointerException("Missing \"classes-location\" member");
	if (simulationInfo.get("info-file") == null)
	    throw new NullPointerException("Missing \"info-file\" member");
	
	Map<String, File> files = new HashMap<>();
	files.put("classes", new File(simulationInfo.getString("classes-location", null)));
	files.put("info", new File(simulationInfo.getString("info-file", null)));
	
	try
	{
	    this.loadingPriority = LoadingPriority.valueOf(simulationInfo.getString("loading-priority", LoadingPriority.DEFAULT.name()).toUpperCase());
	}
	catch (Throwable notFound)
	{
	    WaterKitsuneLogger.warning("Unknown loading priority value: %s", simulationInfo.getString("loading-priority", null).toUpperCase());
	    WaterKitsuneLogger.warning("Fallback to \"%s\" loading priority...", LoadingPriority.DEFAULT.name());
	    this.loadingPriority = LoadingPriority.DEFAULT;
	}
	
	return Collections.unmodifiableMap(files);
    }
    
    public KitsunePlugin getPlugin()
    {
	return this.plugin;
    }
    
    public File getClassesLocation()
    {
	return this.classesLocation;
    }
    
    public LoadingPriority getLoadingPriority()
    {
	return this.loadingPriority;
    }
    
    public static enum LoadingPriority
    {
	HIGH,
	LOW;
	
	public static final LoadingPriority DEFAULT = HIGH;
    }
}
