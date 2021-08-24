package fr.drogonistudio.waterkitsune;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tool to hack modules system.
 * 
 * <p>
 * This tool allow to open and export all defined module. It can be useful for
 * plugins to perform some "illegal" access if it was not allowed by default.
 * </p>
 * 
 * @author DrogoniEntity
 */
public final class OpenExporterModulesHack
{
    /**
     * Prevent instantiation.
     */
    private OpenExporterModulesHack()
    {
    }
    
    /**
     * Open and export all modules.
     * 
     * @param instrumentation
     */
    public static void openExport(Instrumentation instrumentation)
    {
	Set<Module> unnamed = Collections.singleton(ClassLoader.getSystemClassLoader().getUnnamedModule());
	ModuleLayer.boot().modules()
		.forEach((module) -> instrumentation.redefineModule(module, unnamed,
			module.getPackages().stream().collect(Collectors.toMap(Function.identity(), (pkg) -> unnamed)),
			module.getPackages().stream().collect(Collectors.toMap(Function.identity(), (pkg) -> unnamed)),
			Collections.emptySet(), Collections.emptyMap()));
    }
}
