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
