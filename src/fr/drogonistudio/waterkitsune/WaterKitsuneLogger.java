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
    
    public static void debug(String format, Object... args)
    {
	if (System.getProperty("waterkitsune.debug") != null)
	    LOGGER.info(PREFIX.concat(String.format(format, args)));
    }
    
    static
    {
	LOGGER.setLevel(Level.ALL);
    }
}
