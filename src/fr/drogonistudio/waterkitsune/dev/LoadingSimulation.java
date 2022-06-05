package fr.drogonistudio.waterkitsune.dev;

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

import fr.drogonistudio.waterkitsune.plugin.KitsunePlugin;

public class LoadingSimulation
{
    private final File loadingInfoFile;
    
    private File classesLocation;
    private KitsunePlugin plugin;
    
    public LoadingSimulation(File loadingInfoFile) throws NullPointerException, FileNotFoundException
    {
	Objects.requireNonNull(loadingInfoFile, "Loading information file not specified");
	if (loadingInfoFile.exists())
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
}
