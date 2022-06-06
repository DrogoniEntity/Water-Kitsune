<h1><img src="https://repository-images.githubusercontent.com/427630164/8e313f8f-d68f-4c1a-850c-1599a51ee915" width="320" alt="Water-Kitsune"></h1>

## * What is it ?

Water-Kitsune is a patches loader which enable Java program to be modified from different files without modifying shipped program.

It's a Java Agent, which transform loading classes to use modified code by using kitsune plugins.
A kitsune plugin, is an package that contains a piece of code to be run before main program started and some edited classes of the main program.

## * But, how to use it ?

### Launching
During launching process, you need to specify to use Water-Kitsune as agent with JVM parameter `-javaagent` like this :
```
java -javaagent:Water-Kitsune.jar=.enabled # different JVM arguments...
```
Agent's parameter indicate `.enabled` file's path to use.

#### Some launch properties...
Water-Kitsune contains some properties, you can define them with `-D` JVM flag. These properties are :
- `waterkitsune.openexportall`: Open and export all modules to all unnamed modules (useful if modules need to perform some operations but need modules to be opened). Default value set to `false`.
- `waterkistune.debug`: Verbose some additional informations (like class being transformed and if a class has been transformed). Default value set to `false`.
- `waterkitsune.simulate`: Load a specific plugin into "simulation mode". For more informations with simulation mode, please refer to its section.

### `.enabled` file
The `.enabled` file is a text file who indicate which plugin should be loaded. Each line is a file location. You can give a different name and/or place it to another location. Content may look like this :
```
# This one fixes all found bugs
fixes.wkitsune

# This one implement new features
new-features.wkistune
```

Comments line start with `#`. These files can be ZIP/JAR files or directories (by convention, they got `.wkitsune` extension). These paths can be relative or absolute paths. However, the order of these files are important...

### Plugin loading priority
There is a priority system. A file priority will determinate if its class was important to be loaded or not. This priority solve problem in case of two patches want to transform the same class.

The first file has a maximum priority. The second one a priority lower than the first one but upper than the third, etc..

For example, if two patches `A` and `B` want to patch `MyClass`, if `A` is placed before `B`, then `MyClass` will be transformed by `A`'s class version.

## * It's pretty good but how to create my own plugins ?

It's pretty simple ! In your Java project, you will have to setup classpath like how your program will be launched and add agent's JAR into classpath. And you can begin to redefine your classes. However, you will need to configure your execution environment. To be sure your patch will be loaded like in production case, you must set in your `.enabled` file the directory path of your compiled classes.

Patches couldn't transform agent's classes and classes in packages `java.*`.

### Plugin information file (`kitsune-plugin.info`)
To make your plugin to be correctly recognized by loader, you must create a `kitsune-plugin.info` file in the root of your project. This file is a JSON file structured like this:
```json
{
    "name": "Patch's name",
    "description": "Description of your patch, what it does",
    "version": "Patch's version name",
    "initializer": "class's name of your initializer"
}
```
Only `name` member is required. Another members can be omitted.

About `initializer` member, please refer to part "Plugin's Initializer".

### Plugin's Initializer
A plugin initializer will allow to execute some tasks before program's `main` method is invoked. A initializer is class which contains the following method:
```java
public static void initialize(KitsunePlugin plugin);
```
The parameter `plugin` will be your plugin's description.

### Plugin manager
You can access to plugin manager with:
```java
KitsunePluginManager.getManager()
```
This manager allow you to perform some operation on plugins like :
- get a list of all loaded plugins
- stopping your plugin to being loaded by transformer
- retrieve plugin file from a patch description

That's all !

### Testing the plugin (Simulation mode)
The simulation mode allow to bypass classical loading process for development purpose. It will simulate
an packaged plugin and allow to load it from an directory.

In order to use it, you should create an JSON file containing the following content :
```json
{
	"classes-location": "path to your classes (relative or absolute)",
	"info-file": "path to your 'kitsune-plugin.info' (relative or absolute)",
	"loading-priority": "priority over all other plugins (optional)"
}
```
`loading-priority` can have 2 different values:
* `high`: The plugin will be loaded and initialized before other plugins (default value)
* `low`: The plugin will be loaded and initialized after other plugins

Into your JVM arguments, you need to define the system property `waterkitsune.simulate` by setting it to your JSON file.

Let see an example of an launch command :
```
java -javaagent:WaterKitsune.jar=".kitsune-plugins" -Dwaterkitsune.simulate=".kitsune-simulation" -jar program.jar
```
Here, our `.enabled` file is named `.kitsune-plugins` but it is empty. Next, we specify that we will simulate an plugin loading and our configuration file is named `.kitsune-simulation`.

### Light Transformer
From Water-Kitsune 1.2, a new concept of "Light Transformer" has been added. It allow plugins to applied small modifications into a class without needed to rewrite the targeted class completely.

The aim of this utility is to add an better compatibility between other plugins.
If we got 2 plugins whose need to apply some modifications on the same class but they doesn't write in the same location, they should use light transformers for adding their piece of code into the class and so, modifications can be merged without difficulties.
These transformations are applied on the loaded code of an original class or on an rewrote class done by an plugin.

To use it, you will need to create a class which implements the interface `LightKitsuneTransformer`, this interface contains 1 method :
```java
public byte[] transform(String, byte[]);
```
This method will applied a couple of magic operations which will transform incoming class data (`byte[]` parameters) and return transformed data. In some the most case, you may need to use additionnals libraries to perform your magic operations like [ASM](https://asm.ow2.io/) or [Byte Buddy](https://bytebuddy.net/).

After you created your light transformer, to register it, you will need to use `LightKitsuneTransformManager.registerTransformer(String, LightKitsuneTransform)`. Like [Plugin manager](#plugin-manager), you can acceed to this manager with :
```java
public LightKitsuneTransformManager getManager()
```

## Third-party
Water-Kitsune use :
- ["minimal-json"](https://github.com/ralfstx/minimal-json) to perform patches' description parsing
- ["Java Instrumentation API"](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html) for class transformation
- ["Java Logging API"](https://docs.oracle.com/javase/8/docs/api/java/util/logging/package-summary.html) for logging
