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

import java.util.logging.Level;
import java.util.logging.Logger;

public final class WaterKitsuneLogger
{
    private static final String PREFIX = "(Water Kitsune) ";
    private static final Logger LOGGER = Logger.getLogger("Water Kitsune");
    
    private WaterKitsuneLogger()
    {
    }
    
    public static void info(String format, Object... args)
    {
	LOGGER.info(PREFIX.concat(String.format(format, args)));
    }
    
    public static void warning(String format, Object... args)
    {
	LOGGER.warning(PREFIX.concat(String.format(format, args)));
    }
    
    public static void error(String format, Object... args)
    {
	LOGGER.severe(PREFIX.concat(String.format(format, args)));
    }
    
    public static void debug(Level logLevel, String format, Object... args)
    {
	if (System.getProperty("waterkitsune.debug") != null)
	    LOGGER.log(logLevel, PREFIX.concat(String.format(format, args)));
    }
    
    public static void thrown(String message, Throwable throwable)
    {
	LOGGER.log(Level.SEVERE, message, throwable);
    }
    
    static
    {
	LOGGER.setLevel(Level.ALL);
    }
}
