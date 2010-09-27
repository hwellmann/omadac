/*
 * GRIDGAIN - OPEN CLOUD PLATFORM.
 * COPYRIGHT (C) 2005-2008 GRIDGAIN SYSTEMS. ALL RIGHTS RESERVED.
 *
 * THIS IS FREE SOFTWARE; YOU CAN REDISTRIBUTE IT AND/OR
 * MODIFY IT UNDER THE TERMS OF THE GNU LESSER GENERAL PUBLIC
 * LICENSE AS PUBLISHED BY THE FREE SOFTWARE FOUNDATION; EITHER
 * VERSION 2.1 OF THE LICENSE, OR (AT YOUR OPTION) ANY LATER
 * VERSION.
 *
 * THIS LIBRARY IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  SEE THE
 * GNU LESSER GENERAL PUBLIC LICENSE FOR MORE DETAILS.
 *
 * YOU SHOULD HAVE RECEIVED A COPY OF THE GNU LESSER GENERAL PUBLIC
 * LICENSE ALONG WITH THIS LIBRARY; IF NOT, WRITE TO THE FREE
 * SOFTWARE FOUNDATION, INC., 51 FRANKLIN ST, FIFTH FLOOR, BOSTON, MA
 * 02110-1301 USA
 */

package org.gridgain.grid.spi.deployment.uri;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Loads classes and resources from the GAR file or "unpacked" GAR file
 * (GAR directory).
 * <p>
 * Class loader scans GAR file or GAR directory first and than if
 * class/resource was not found scans all JAR files.
 * It is assumed that all libraries are in the {@link #DFLT_LIBS_DIR_PATH}
 * directory.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@SuppressWarnings({"CustomClassloader"})
class GridUriDeploymentClassLoader extends ClassLoader {
    /** Libraries directory default value (value is <tt>lib</tt>). */
    public static final String DFLT_LIBS_DIR_PATH = "lib";

    /** */
    private static final String GRIDGAIN_JAR_SIGNATURE = "org/gridgain/grid/GridTask.class";

    /** */
    private final List<String> ignoredJars = new ArrayList<String>();

    /** Either GAR file or directory with unpacked GAR file. */
    private final File file;

    /** Logger. */
    private final GridLogger log;

    /**
     * Creates new instance of class loader.
     *
     * @param parent Parent class loader.
     * @param file GAR file or directory with unpacked GAR file.
     * @param log Logger.
     */
    GridUriDeploymentClassLoader(ClassLoader parent, File file, GridLogger log) {
        super(parent);

        assert parent != null : "ASSERTION [line=70, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentClassLoader.java]";
        assert file != null : "ASSERTION [line=71, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentClassLoader.java]";
        assert log != null : "ASSERTION [line=72, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentClassLoader.java]";

        this.file = file;
        this.log = log;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"UnusedCatchParameter"})
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        //noinspection CatchGenericClass
        try {
            // First, check if the class has already been loaded
            Class<?> cls = findLoadedClass(name);

            if (cls == null) {
                try {
                    // Search classes in GAR file.
                    cls = findGarClass(name);
                }
                catch (ClassNotFoundException e) {
                    // If still not found, then invoke parent class loader in order
                    // to find the class.
                    cls = loadClass(name, true);
                }
            }

            return cls;
        }
        catch (ClassNotFoundException e) {
            throw e;
        }
        // Catch Throwable to secure against any errors resulted from
        // corrupted class definitions or other user errors.
        catch (Throwable e) {
            throw new ClassNotFoundException("Failed to load class due to unexpected error: " + name, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"UnusedCatchParameter"})
    public Class<?> loadClassGarOnly(String name) throws ClassNotFoundException {
        //noinspection CatchGenericClass
        try {
            // First, check if the class has already been loaded
            Class<?> cls = findLoadedClass(name);

            if (cls == null) {
                // Search classes in GAR file.
                cls = findGarClass(name);
            }

            return cls;
        }
        catch (ClassNotFoundException e) {
            throw e;
        }
        // Catch Throwable to secure against any errors resulted from
        // corrupted class definitions or other user errors.
        catch (Throwable e) {
            throw new ClassNotFoundException("Failed to load class due to unexpected error: " + name, e);
        }
    }

    /**
     * Search class in GAR file.
     *
     * @param name Class name.
     * @return Class object.
     * @throws ClassNotFoundException If class not found.
     */
    protected Class<?> findGarClass(String name) throws ClassNotFoundException {
        String path = name.replaceAll("\\.", "/") + ".class";

        GridByteArrayList data = null;

        try {
            if (file.isDirectory() == true) {
                data = loadFromGarDir(path);
            }
            else {
                data = loadFromGarFile(path);
            }
        }
        catch (IOException e) {
            throw new ClassNotFoundException("Failed to find class: " + name, e);
        }

        if (data == null) {
            throw new ClassNotFoundException("Class not found: " + name);
        }

        return defineClass(name, data.getInternalArray(), 0, data.getSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream in = null;

        // Find resource in GAR file first.
        in = getResourceAsStreamGarOnly(name);

        // Find resource in parent class loader.
        if (in == null) {
            in = ClassLoader.getSystemResourceAsStream(name);
        }

        if (in == null) {
            in = super.getResourceAsStream(name);
        }

        return in;
    }

    /**
     * Returns an input stream for reading the specified resource from GAR file only.
     * @param name Resource name.
     * @return An input stream for reading the resource, or <tt>null</tt>
     *  if the resource could not be found.
     */
    public InputStream getResourceAsStreamGarOnly(String name) {
        InputStream in = null;

        // Find resource in GAR file first.
        String path = name.replaceAll("\\\\", "/");

        try {
            GridByteArrayList data = null;

            if (file.isDirectory() == true) {
                data = loadFromGarDir(path);
            }
            else {
                data = loadFromGarFile(path);
            }

            if (data != null) {
                in = new ByteArrayInputStream(data.getInternalArray(), 0, data.getSize());
            }
        }
        catch (IOException e) {
            log.warning("Failed to get resource [name=" + name + ", file=" + file + ']', e);

            return null;
        }

        return in;
    }

    /**
     * Loads certain file from GAR file.
     * <p>
     * Method scans file for the entry with the given name and returns
     * it back if there is one. If there is no entry with this name it
     * scans all JAR libraries from {@link #DFLT_LIBS_DIR_PATH} directory
     * and process them trying to find given resource or class.
     *
     * @param path File name which should be loaded.
     * @return Either Resource/class as bytes or <tt>null</tt> if there is
     *      no such one.
     * @throws IOException Thrown if reading error happens.
     */
    private GridByteArrayList loadFromGarFile(String path) throws IOException {
        ZipFile gar = new ZipFile(file);

        ZipEntry entry = gar.getEntry(path);

        if (entry == null) {
            ZipEntry libs = gar.getEntry(DFLT_LIBS_DIR_PATH);

            if (libs != null) {
                for (ZipEntry garEntry : GridUtils.asIterable(gar.entries())) {
                    if (garEntry.getName().startsWith(DFLT_LIBS_DIR_PATH) == true &&
                        garEntry.getName().endsWith(".jar") == true &&
                        ignoredJars.contains(garEntry.getName()) == false) {

                        // We should ignore gridgain.jar in GAR's /lib folder.
                        boolean isGridGainJar = loadFromJarFile(gar.getInputStream(garEntry),
                            GRIDGAIN_JAR_SIGNATURE) != null;

                        if (isGridGainJar == true) {
                            ignoredJars.add(garEntry.getName());

                            log.warning("Found jar file in /" + DFLT_LIBS_DIR_PATH + " folder " +
                                "that contains GridTask interface. GAR files should not include gridgain.jar in " +
                                "their /" + DFLT_LIBS_DIR_PATH + " folder. Will safely ignore it [GAR file = " +
                                file.getName() + ", JAR file = " + garEntry.getName() + ']');

                            return null;
                        }

                        // Read JAR.
                        GridByteArrayList data = loadFromJarFile(gar.getInputStream(garEntry), path);

                        if (data != null) {
                            return data;
                        }
                    }
                }
            }

            return null;
        }

        GridByteArrayList data = entry.getSize() > 0 ? new GridByteArrayList((int)entry.getSize()) :
            new GridByteArrayList(1024);

        InputStream in = new BufferedInputStream(gar.getInputStream(entry));

        try {
            data.readAll(in);

            return data;
        }
        finally {
            GridUtils.close(in, log);
        }
    }

    /**
     * Scans given input stream trying to find class or resource with given name.
     *
     * @param in Input stream.
     * @param path Resource or class file name.
     * @return Resource or class as bytes.
     * @throws IOException Thrown if any stream reading error happens.
     */
    private GridByteArrayList loadFromJarFile(InputStream in, String path) throws IOException {
        ZipInputStream zin = new ZipInputStream(new BufferedInputStream(in));

        try {
            ZipEntry jarEntry = zin.getNextEntry();

            while(jarEntry != null) {
                // Class found in JAR.
                if (jarEntry.getName().equals(path) == true) {
                    GridByteArrayList data = jarEntry.getSize() > 0 ? new GridByteArrayList((int)jarEntry.getSize()) :
                        new GridByteArrayList(1024);

                    data.readAll(zin);

                    return data;
                }

                zin.closeEntry();

                jarEntry = zin.getNextEntry();
            }
        }
        finally {
            GridUtils.close(zin, log);
        }

        return null;
    }

    /**
     * Load class or resource from the GAR directory. It is assumed that
     * GAR directory has the same structure as unpacked GAR file. Thus
     * Method tries to find file with given name first and return it.
     * If there is no such file it scans {@link #DFLT_LIBS_DIR_PATH}
     * directory for the JAR files and tries to find given class or resource
     * there. If nothing found returns <tt>null</tt>
     *
     * @param path Class or resource file name.
     * @return Either class/resource as bytes if it was found or <tt>null</tt>
     *      if there is no such class/resource.
     * @throws IOException Thrown if any reading error happens.
     */
    private GridByteArrayList loadFromGarDir(String path) throws IOException {
        File classFile = findInDir(file, file, path);

        if (classFile == null) {
            File libs = new File(file, DFLT_LIBS_DIR_PATH);

            if (libs.exists() == true && libs.isDirectory() == true) {
                File[] jars = libs.listFiles(new FilenameFilter() {
                    /**
                     * {@inheritDoc}
                     */
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".jar") == true;
                    }
                });

                String zipPath = "/".equals(File.separator) == true ? path : path.replaceAll("\\\\", "/");

                for (File jar : jars) {
                    // Read JAR.
                    GridByteArrayList data = loadFromJarFile(new FileInputStream(jar), zipPath);

                    if (data != null) {
                        return data;
                    }
                }
            }

            return null;
        }

        InputStream in = new BufferedInputStream(new FileInputStream(classFile));

        try {
            GridByteArrayList data = new GridByteArrayList((int)classFile.length());

            data.readAll(in);

            return data;
        }
        finally {
            GridUtils.close(in, log);
        }
    }

    /**
     * Recursively scans given directory for the file.
     *
     * @param root Root directory (upper level directory).
     * @param dir  Directory which should be scanned by this step.
     * @param path File name we are looking for.
     * @return Either file or <tt>null</tt> if file was not found.
     */
    private File findInDir(File root, File dir, String path) {
        File[] files = dir.listFiles();

        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isDirectory() == true) {
                    File found = findInDir(root, file, path);

                    if (found != null) {
                        return found;
                    }
                }
                else {
                    File target = new File(root, path);

                    if (file.equals(target) == true) {
                        return file;
                    }
                }
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridUriDeploymentClassLoader.class, this);
    }
}
