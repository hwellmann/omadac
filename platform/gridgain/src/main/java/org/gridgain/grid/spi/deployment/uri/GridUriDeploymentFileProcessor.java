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
import java.lang.reflect.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.deployment.uri.util.*;

/**
 * Utility class.
 * <p>
 * Provides useful and common functions for URI deployment.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
final class GridUriDeploymentFileProcessor {
    /**
     * Enforces singleton.
     */
    private GridUriDeploymentFileProcessor() {
        // No-op.
    }

    /**
     * Method processes given GAR file and extracts all tasks from it which are
     * either mentioned in GAR descriptor or implements interface {@link GridTask}
     * if there is no descriptor in file.
     *
     * @param file GAR file with tasks.
     * @param uri GAR file deployment URI.
     * @param log Logger.
     * @throws GridSpiException Thrown if file could not be read.
     * @return List of tasks from given file.
     */
    static GridUriDeploymentFileProcessorResult processFile(File file, String uri, GridLogger log)
        throws GridSpiException {
        if (file.isDirectory() == true) {
            try {
                File xml = new File(file, GridUriDeploymentSpi.XML_DESCRIPTOR_PATH);

                if (xml.exists() == false || xml.isDirectory() == true) {
                    if (log.isInfoEnabled() == true) {
                        log.info("Process deployment without descriptor file [path=" +
                            GridUriDeploymentSpi.XML_DESCRIPTOR_PATH + ", file=" + file.getAbsolutePath() + ']');
                    }

                    if (checkIntegrity(file, log) == true) {
                        return processNoDescriptorFile(file, uri, log);
                    }

                    log.error("Tasks in GAR file not loaded in configuration. Invalid file signature. [uri=" +
                        GridUriDeploymentUtils.hidePassword(uri) + ']');
                }
                else {
                    InputStream in = null;

                    try {
                        in = new BufferedInputStream(new FileInputStream(xml));

                        // Parse XML task definitions and add them to cache.
                        GridUriDeploymentSpringDocument doc = GridUriDeploymentSpringParser.parseTasksDocument(in, log);

                        assert doc != null : "ASSERTION [line=89, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentFileProcessor.java]";

                        // Warning. No security check for directory.
                        if (checkIntegrity(file, log) == true) {
                            return processWithDescriptorFile(doc, file, uri, log);
                        }

                        log.error("Tasks in GAR file not loaded in configuration. Invalid file signature. [uri=" +
                            GridUriDeploymentUtils.hidePassword(uri) + ']');
                    }
                    finally {
                        GridUtils.close(in, log);
                    }
                }
            }
            catch (IOException e) {
                throw (GridSpiException)new GridSpiException("IO error when parsing GAR directory: " + file.getAbsolutePath(), e).setData(105, "src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentFileProcessor.java");
            }
        }
        // Process GAR files.
        else {
            try {
                ZipFile zip = new ZipFile(file);

                ZipEntry entry = zip.getEntry(GridUriDeploymentSpi.XML_DESCRIPTOR_PATH);

                if (entry == null) {
                    if (log.isInfoEnabled() == true) {
                        log.info("Process deployment without descriptor file [path=" +
                            GridUriDeploymentSpi.XML_DESCRIPTOR_PATH + ", file=" + file.getAbsolutePath() + ']');
                    }

                    if (checkIntegrity(file, log) == true) {
                        return processNoDescriptorFile(file, uri, log);
                    }

                    log.error("Tasks in GAR file not loaded in configuration. Invalid file signature. [uri=" +
                        GridUriDeploymentUtils.hidePassword(uri) + ']');
                }
                else {
                    InputStream in = null;

                    try {
                        in = new BufferedInputStream(zip.getInputStream(entry));

                        // Parse XML task definitions and add them to cache.
                        GridUriDeploymentSpringDocument doc = GridUriDeploymentSpringParser.parseTasksDocument(in,
                            log);

                        assert doc != null : "ASSERTION [line=138, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentFileProcessor.java]";

                        if (checkIntegrity(file, log) == true) {
                            return processWithDescriptorFile(doc, file, uri, log);
                        }

                        log.error("Tasks in GAR file not loaded in configuration. Invalid file signature. [uri=" +
                            GridUriDeploymentUtils.hidePassword(uri) + ']');
                    }
                    finally {
                        GridUtils.close(in, log);
                    }
                }
            }
            catch (IOException e) {
                throw (GridSpiException)new GridSpiException("IO error when parsing GAR file: " + file.getAbsolutePath(), e).setData(153, "src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentFileProcessor.java");
            }
        }

        return null;
    }

    /**
     * Processes given GAR file and returns back all tasks which are in
     * descriptor.
     *
     * @param doc GAR file descriptor.
     * @param file GAR file.
     * @param uri GAR file deployment URI.
     * @param log Logger.
     * @throws GridSpiException Thrown if it's impossible to open file.
     * @return List of tasks from descriptor.
     */
    @SuppressWarnings({"ClassLoader2Instantiation"})
    private static GridUriDeploymentFileProcessorResult processWithDescriptorFile(GridUriDeploymentSpringDocument doc,
        File file, String uri, GridLogger log) throws GridSpiException {
        GridUriDeploymentClassLoader clsLdr = new GridUriDeploymentClassLoader(
            GridUriDeploymentClassLoader.class.getClassLoader(), file, log);

        List<Class<? extends GridTask<?, ?>>> tasks = doc.getTasks(clsLdr);

        List<Class<? extends GridTask<?, ?>>> validTasks = null;

        if (tasks != null && tasks.size() > 0) {
            validTasks = new ArrayList<Class<? extends GridTask<?, ?>>>();

            for (Class<? extends GridTask<?, ?>> task : tasks) {
                if (isAllowedTaskClass(task) == false) {
                    log.warning("Failed to load task. Task should be public none-abstract class " +
                        "(might be inner static one) that implements GridTask interface [taskCls=" + task + ']');
                }
                else {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Found grid deployment task: " + task.getName());
                    }

                    validTasks.add(task);
                }
            }
        }

        GridUriDeploymentFileProcessorResult res = new GridUriDeploymentFileProcessorResult();

        res.setClassLoader(clsLdr);

        if (validTasks != null && validTasks.size() > 0) {
            res.setTaskClasses(validTasks);
        }
        else if (log.isDebugEnabled() == true) {
            log.debug("No tasks loaded from file [file=" + file.getAbsolutePath() +
                ", uri=" + GridUriDeploymentUtils.hidePassword(uri) + ']');
        }

        return res;
    }

    /**
     * Processes GAR files which have no descriptor. It scans every class and
     * checks if it is a valid task or not. All valid tasks are returned back.
     *
     * @param file GAR file or directory.
     * @param uri GAR file deployment URI.
     * @param log Logger.
     * @throws GridSpiException Thrown if file reading error happened.
     * @return List of tasks from given file.
    */
    @SuppressWarnings("ClassLoader2Instantiation")
    private static GridUriDeploymentFileProcessorResult processNoDescriptorFile(File file, String uri, GridLogger log)
        throws GridSpiException {
        GridUriDeploymentClassLoader clsLdr = new GridUriDeploymentClassLoader(
            GridUriDeploymentClassLoader.class.getClassLoader(), file, log);

        Set<Class<? extends GridTask<?, ?>>> clss = GridUriDeploymentDiscovery.getClasses(clsLdr, file);

        GridUriDeploymentFileProcessorResult res = new GridUriDeploymentFileProcessorResult();

        res.setClassLoader(clsLdr);

        if (clss != null) {
            List<Class<? extends GridTask<?, ?>>> validTasks =
                new ArrayList<Class<? extends GridTask<?,?>>>(clss.size());

            for (Class<? extends GridTask<?, ?>> cls : clss) {
                if (isAllowedTaskClass(cls) == true) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Found grid deployment task: " + cls.getName());
                    }

                    validTasks.add(cls);
                }
            }

            if (validTasks.size() > 0) {
                res.setTaskClasses(validTasks);
            }
            else if (log.isDebugEnabled() == true) {
                log.debug("No tasks loaded from file [file=" + file.getAbsolutePath() +
                    ", uri=" + GridUriDeploymentUtils.hidePassword(uri) + ']');
            }
        }

        return res;
    }

    /**
     * Check that class may be instantiated as {@link GridTask} and used
     * in deployment.
     *
     * Loaded task class must implement interface {@link GridTask}.
     * Only non-abstract, non-interfaces and public classes allowed.
     * Inner static classes also allowed for loading.
     *
     * @param cls Class to check
     * @return <tt>true</tt> if class allowed for deployment.
     */
    private static boolean isAllowedTaskClass(Class<?> cls) {
        if (GridTask.class.isAssignableFrom(cls) == false) {
            return false;
        }

        int modifiers = cls.getModifiers();

        return Modifier.isAbstract(modifiers) == false && Modifier.isInterface(modifiers) == false &&
            (cls.isMemberClass() == false || Modifier.isStatic(modifiers) == true) &&
            Modifier.isPublic(modifiers) == true;

    }

    /**
     * Make integrity check for GAR file.
     * Method returns <tt>false</tt> if GAR file has incorrect signature.
     *
     * @param file GAR file which should be verified.
     * @param log Logger.
     * @return <tt>true</tt> if given file is a directory of verification
     *      completed successfully otherwise returns <tt>false</tt>.
     */
    private static boolean checkIntegrity(File file, GridLogger log) {
        try {
            return file.isDirectory() == true ||
                GridUriDeploymentJarVerifier.verify(file.getAbsolutePath(), false, log) == true;
        }
        catch (IOException e) {
            log.error("Error while making integrity file check.", e);
        }

        return false;
    }
}
