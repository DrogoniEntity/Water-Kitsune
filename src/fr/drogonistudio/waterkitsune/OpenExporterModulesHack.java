package fr.drogonistudio.waterkitsune;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class OpenExporterModulesHack
{
    
    private OpenExporterModulesHack()
    {
    }
    
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
