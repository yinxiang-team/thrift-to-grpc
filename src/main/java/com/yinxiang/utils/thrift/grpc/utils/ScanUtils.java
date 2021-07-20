package com.yinxiang.utils.thrift.grpc.utils;

import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarEntry;

import static java.lang.Thread.currentThread;

/**
 * The utils for scan classes.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public class ScanUtils {
  /**
   * Scan classes in a package.
   * @param packageName package name
   * @param filter      class filter
   * @return  a set of classes
   * @throws IOException            IOException
   * @throws ClassNotFoundException ClassNotFoundException
   */
  public static Set<Class<?>> scanClass(String packageName, Function<Class<?>, Boolean> filter)
          throws IOException, ClassNotFoundException {
    Set<Class<?>> classes = Sets.newLinkedHashSet();
    // format package
    String packagePath = packageName.replace(".", "/");
    // scan in resources
    Enumeration<URL> resources = currentThread().getContextClassLoader().getResources(packagePath);
    while (resources.hasMoreElements()) {
      URL url = resources.nextElement();
      if (url.getProtocol().equals("file")) { // dir
        // create dir
        File dir = new File(URLDecoder.decode(url.getFile(), "UTF-8"));
        // scan in dir
        if (dir.exists() && dir.isDirectory()) {
          scanInDir(classes, dir, packageName, filter);
        }
      } else if (url.getProtocol().equals("jar")){ // jar
        // scan in jar
        Enumeration<JarEntry> entries = ((JarURLConnection) url.openConnection()).getJarFile().entries();
        while (entries.hasMoreElements()) {
          JarEntry entry = entries.nextElement();
          if (entry.isDirectory()) {
            continue;
          }
          String name = entry.getName();
          // format name
          if (name.charAt(0) == '/') {
            name = name.substring(1);
          }
          // check package and '/'
          int idx = name.lastIndexOf('/');
          if (!name.startsWith(packagePath) || idx == -1) {
            continue;
          }
          // load class
          if (name.endsWith(".class")) {
            load(classes, name.replace('/', '.'), filter);
          }
        }
      }
    }
    return classes;
  }

  /**
   * Filter class.
   * @param classes   a set to add classes
   * @param className class name
   * @param filter    class filter
   * @throws ClassNotFoundException
   */
  private static void load(Set<Class<?>> classes, String className, Function<Class<?>, Boolean> filter)
          throws ClassNotFoundException {
    // load class by name
    Class<?> clz = currentThread().getContextClassLoader().loadClass(className.substring(0, className.length() - 6));
    // load
    if (filter.apply(clz)) {
      classes.add(clz);
    }
  }

  /**
   * Scan classes in a direction.
   * @param classes     a set to add classes
   * @param dir         a direction
   * @param packageName package name
   * @param filter      class filter
   * @throws ClassNotFoundException exception
   */
  private static void scanInDir(
          Set<Class<?>> classes,
          File dir,
          String packageName,
          Function<Class<?>, Boolean> filter
  ) throws ClassNotFoundException {
    File[] files = dir.listFiles();
    if (files == null) {
      return;
    }
    // find
    for (File file : files) {
      // make full name
      String fullName = packageName + "." + file.getName();
      if (file.isDirectory()) { // dir
        scanInDir(classes, file, fullName, filter);
      } else if (fullName.endsWith(".class")) { // class file
        load(classes, fullName, filter);
      }
    }
  }
}
