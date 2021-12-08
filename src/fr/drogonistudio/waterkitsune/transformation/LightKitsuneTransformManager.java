package fr.drogonistudio.waterkitsune.transformation;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import fr.drogonistudio.waterkitsune.WaterKitsuneLogger;
import fr.drogonistudio.waterkitsune.plugin.KitsunePluginManager;
import fr.drogonistudio.waterkitsune.plugin.KitsunePluginManager.PluginLoadingStatement;

/**
 * {@link LightKistuneTransformer} manager.
 * 
 * <p>
 * This manager will handle any light transformer and apply transformation when
 * a class will be redefined.
 * </p>
 * 
 * <p>
 * Transformers are stored in a dedicated set and each class to transform got an
 * set of transformers. During its life-cycle, transformer registrations are
 * available only during plugins initialization process. After that, it will be
 * locked and transformers couldn't be added or remove. You should register all
 * of your transformer in your plugin's initializer.
 * </p>
 * 
 * <p>
 * A unique instance of this manager is available with {@link #getManager()} and
 * transformer could be registered or removed with
 * {@link #registerTransformer(Class, LightKitsuneTransformer)} and
 * {@link #unregisterTransformer(Class, LightKitsuneTransformer)}.
 * </p>
 * 
 * @author DrogoniEntity
 * @see LightKitsuneTransformer
 */
public class LightKitsuneTransformManager
{
    /**
     * Unique instance of this manager.
     */
    private static LightKitsuneTransformManager instance;
    
    /**
     * Transformer registry.
     * 
     * <p>
     * Each registry are stored in concurrent set, which these ones are assigned
     * with a class.
     * </p>
     */
    private final Map<String, Set<LightKitsuneTransformer>> transformers;
    
    /**
     * Registration locker.
     */
    private final AtomicBoolean registerationLocked;
    
    /**
     * Initialize manager.
     */
    public LightKitsuneTransformManager()
    {
	if (instance != null)
	    throw new IllegalStateException("Light transformer manager already created");
	instance = this;
	
	this.transformers = new ConcurrentHashMap<>();
	this.registerationLocked = new AtomicBoolean(false);
    }
    
    /**
     * Register {@code transformer}.
     * 
     * <p>
     * It will assign {@code transformer} to be used to transform
     * {@code targetClass}. {@link LightKitsuneTransformer#transform(Class, byte[])
     * transform(Class, byte[])} will be invoked only if {@code targetClass} is
     * being transformed.
     * </p>
     * 
     * <p>
     * You can cancel this action with
     * {@link #unregisterTransformer(Class, LightKitsuneTransformer)} or register
     * {@code transformer} again but with another class to transform.
     * </p>
     * 
     * @param targetClass
     *            - class to transform
     * @param transformer
     *            - transformer to register
     * @throws IllegalStateException
     *             if registration are locked
     * @throws NullPointerException
     *             if {@code transformer} is null
     */
    public synchronized void registerTransformer(Class<?> targetClass, LightKitsuneTransformer transformer)
	    throws IllegalStateException, NullPointerException
    {
	this.checkRegistrationsLocked();
	if (transformer == null)
	    throw new NullPointerException("transformer is null");
	
	Set<LightKitsuneTransformer> transformerSet;
	if (!this.transformers.containsKey(targetClass.getName()))
	{
	    transformerSet = ConcurrentHashMap.newKeySet();
	    this.transformers.put(targetClass.getName(), transformerSet);
	} else
	{
	    transformerSet = this.transformers.get(targetClass.getName());
	}
	
	transformerSet.add(transformer);
    }
    
    /**
     * Remove {@code transformer} from registry.
     * 
     * <p>
     * If {@code transformer} has been registered with {@code targetClass}, it will
     * be removed from registry. However, it still registered if it has been
     * registered with another class.
     * </p>
     * 
     * @param targetClass
     *            - class to transform
     * @param transformer
     *            - transformer to remove
     * @throws IllegalStateException
     *             if registrations are locked
     * @throws NullPointerException
     *             if {@code transformer} is null
     */
    public synchronized void unregisterTransformer(Class<?> targetClass, LightKitsuneTransformer transformer)
	    throws IllegalStateException
    {
	this.checkRegistrationsLocked();
	if (transformer == null)
	    throw new NullPointerException("transformer is null");
	
	if (this.transformers.containsKey(targetClass.getName()))
	{
	    this.transformers.get(targetClass.getName()).remove(transformer);
	}
    }
    
    /**
     * Apply transformers to {@code classBeingRedefined}.
     * 
     * <p>
     * It will apply transformer only there is registered transformers to
     * {@code classBeingRedefined}. If transformer return {@code null} during
     * process, it will be ignored.
     * </p>
     * 
     * <p>
     * This method should be used only by agent.
     * </p>
     * 
     * @param classBeingRedefined
     *            - class to transform
     * @param classFileBuffer
     *            - incoming class data
     * @return transformed class data
     */
    public synchronized byte[] apply(String className, byte classFileBuffer[])
    {
	// Apply only if there is some registered transformed
	if (this.transformers.containsKey(className))
	{
	    Iterator<LightKitsuneTransformer> iterator = this.transformers.get(className).iterator();
	    while (iterator.hasNext())
	    {
		LightKitsuneTransformer transformer = iterator.next();
		String transformerName = transformer.getClass().getName();
		WaterKitsuneLogger.debug(Level.FINE, "Applying light transformer \"%s\"...", transformerName);
		
		try
		{
		    byte nextBuffer[] = transformer.transform(className, classFileBuffer);
		    
		    // Transformation will be effective only if transformer return something
		    if (nextBuffer != null)
			classFileBuffer = nextBuffer;
		} catch (Throwable t)
		{
		    WaterKitsuneLogger.error("Failed to apply light transformer \"%s\"", transformerName);
		    t.printStackTrace();
		}
	    }
	}
	
	return classFileBuffer;
    }
    
    /**
     * Checking if registrations are locked.
     * 
     * <p>
     * If their are locked, an exception will be thrown.
     * </p>
     * 
     * @throws IllegalStateException
     *             if registrations are locked
     */
    private void checkRegistrationsLocked() throws IllegalStateException
    {
	if (this.registerationLocked.get())
	    throw new IllegalStateException("transformers registry is locked");
    }
    
    /**
     * Lock registrations.
     * 
     * <p>
     * This method act only if plugins are loaded. It should be used only by agent
     * for internal purpose.
     * </p>
     */
    public void lockRegistrations()
    {
	if (KitsunePluginManager.getManager().getLoadingStatement() == PluginLoadingStatement.LOADED)
	    this.registerationLocked.set(true);
    }
    
    /**
     * Getting the unique instance {@link LightKitsuneTransformer} manager.
     * 
     * @return the unique instance {@link LightKitsuneTransformer} manager
     */
    public static LightKitsuneTransformManager getManager()
    {
	return instance;
    }
}
