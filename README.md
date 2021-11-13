<h1><img src="https://repository-images.githubusercontent.com/427630164/8e313f8f-d68f-4c1a-850c-1599a51ee915" width="320" alt="Water-Kitsune"></h1>

## * What is it ?

Water-Kitsune is a patches loader which enable Java program to be modified from different files without modifying shipped program.

It's a Java Agent, which transform loading classes to use modified code by used patches. A patch, is a simply a set of Java classes to be use instead of program's classes. However, patches can run some special tasks before main method will be invoked.

## * But, how to use it ?

### Launching
During launching process, you need to specify to use Water-Kitsune as agent with JVM parameter `-javaagent` like this :
```
java -javaagent:Water-Kitsune.jar=.enabled # differents JVM arguments...
```
Agent's parameter indicate `.enabled` file's path to use.

#### Some launch properties...
Water-Kitsune contains some properties, you can define them with `-D` JVM flag. These properties are :
- `waterkitsune.openexportall`: Open and export all modules to all unnamed modules (usefull if modules need to perform some operations but need modules to be opened). Default value set to `false`.
- `waterkistune.debug`: Verbose some additionnal informations (like class being transformed and if a class has been transformed). Default value set to `false`.

### `.enabled` file
The `.enabled` file is a text file who indicate which patches should be loaded. Each line is a file location. You can give a different name and/or place it to another location. Content may look like this :
```
# This one fixes all found bugs
fixes.jar

# This one implement new features
new-features.jar
```

Comments line start with `#`. These files can be ZIP/JAR files or directories. These paths can be relative or absolute paths. However, the order of these files are important...

### Patch loading priority
There is a priority system. A file priority will determinate if its class was important to be loaded or not. This priority solve problem in case of two patches want to transform the same class.

The first file has a maximum priority. The second one a priority lower than the first one but upper than the third, etc..

For example, if two patches `A` and `B` want to patch `MyClass`, if `A` is placed before `B`, then `MyClass` will be transformed by `A`'s class version.

## * It's good but how to create my own patches ?

It's pretty simple ! In your Java project, you will have to setup classpath like how your progam will be launched and add agent's JAR into classpath. And you can begin to redefine your classes. However, you will need to configure your execution environment. To be sure your patch will be loaded like in production case, you must set in your `.enabled` file the directory path of your compiled classes.

Patches couldn't transform agent's classes and classes in packages `java.*`.

### Patch description (`plugin.kitmeta`)
To make your patch to be correctly recognized by loader, you must create a `plugin.kitmeta` file in the root of your project. This file is a JSON file structured like this:
```json
{
    "name": "Patch's name",
    "description": "Description of your patch, what it does",
    "version": "Patch's version name",
    "initializer": "class's name of your initializer"
}
```
Only `name` member is required. Another members can be omitted.

About `initializer` member, please refer to part "Patch's Initializer".

### Patch's Initializer
A patch initializer will allow to execute some tasks before program's `main` method is invoked. A initializer is class which contains the following method:
```java
public static void initialize(KitsunePlugin plugin);
```
The parameter `plugin` will be your patch's description.

### Patch manager
You can access to patch manager with:
```java
KitsunePluginManager.getManager()
```
This manager allow you to perform some operation on patches like :
- get a list of all loaded patches
- stopping your patch to being loaded by transformer
- retrieve patch file from a patch description

That's all !

## Third-party
Water-Kitsune use :
- ["minimal-json"](https://github.com/ralfstx/minimal-json) to perform patches' description parsing
- ["Java Instrumentation API"](https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html) for class transformation
- ["Java Logging API"](https://docs.oracle.com/javase/8/docs/api/java/util/logging/package-summary.html) for logging
