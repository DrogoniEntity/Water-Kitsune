package fr.drogonistudio.waterkitsune.transformation;

/**
 * Ability to apply custom transformations on classes.
 * 
 * <p>
 * This interface will allow plugins to perform tiny change without need to
 * rewrote completely targeted class. Light transformer can be registered into
 * {@link LightKitsuneTransformManager}.
 * </p>
 * 
 * <p>
 * When agent will transform some classes, {@link #transform(Class, byte[])}
 * will be invoked by the agent with the representation of class which will be
 * transformed and its current wrote data. These transformation are done after
 * being loaded from plugins' file.
 * </p>
 * 
 * @author DrogoniEntity
 * @see LightKitsuneTransformManager
 */
public interface LightKitsuneTransformer
{
    /**
     * Transforming {@code classBeingRedefined}.
     * 
     * <p>
     * This transformer may perform some transformation into class's file buffer by
     * modifying its structure/code.
     * </p>
     * 
     * <p>
     * This method will be invoked by agent only if this transformer has been
     * registered from {@link LightKitsuneTransformManager}.
     * </p>
     * 
     * @param className - class's name
     * @param classFileBuffer - class's data
     * @return new class data (transformed class data)
     */
    public byte[] transform(String className, byte classFileBuffer[]);
}
