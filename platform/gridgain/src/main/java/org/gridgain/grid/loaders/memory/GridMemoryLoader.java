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

package org.gridgain.grid.loaders.memory;

import org.gridgain.grid.*;
import org.gridgain.grid.loaders.*;
import org.gridgain.grid.util.*;
import org.springframework.beans.factory.xml.*;
import org.springframework.beans.*;
import org.springframework.core.io.*;
import java.io.*;
import java.util.*;

/**
 * This class defines in-memory loader. Basically it provides convenient and often used way to
 * load GridGain from existing Java process by passing in a Spring XML configuration file that
 * should have one bean of type {@link GridConfiguration}. This is the standard way to launch
 * GridGain from user's applications.
 * <p>
 * Note that usage of this class is largely deprecated since you can start GridGain with Spring
 * configuration file directly through {@link GridFactory#start(String)} method. This loader is
 * mainly preserved for backward compatibility.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see GridFactory#start(String)
 */
@Deprecated
@GridLoader(description = "In-memory loader")
public final class GridMemoryLoader {
    /** GRIDGAIN_HOME variable name. */
    private static final String GRIDGAIN_HOME = "GRIDGAIN_HOME";

    /**
     * Enforces singleton.
     *
     */
    private GridMemoryLoader() {
        // No-op.
    }

    /**
     * Loads GridGain with specified Spring XML configuration file and optional grid
     * instance name.
     *
     * @param springXmlPath Absolute or relative to GRIDGAIN_HOME path of Spring XML configuration
     *      file. This file should contain one bean of type {@link GridConfiguration}.
     * @throws GridException Thrown in case of any errors.
     */
    // Warning suppression is due to Spring...
    @SuppressWarnings({"unchecked", "RawUseOfParameterizedType"})
    public static void loadGridGain(String springXmlPath) throws GridException {
        GridArgumentCheck.checkNull(springXmlPath, "springXmlPath");

        File path = new File(springXmlPath);

        if (path.exists() == false) {
            // Not an absolute or valid relative path.
            // Try relative to GRIDGAIN_HOME.
            String ggHome = System.getProperty(GRIDGAIN_HOME);

            if (ggHome == null) {
                ggHome = System.getenv(GRIDGAIN_HOME);

                if (ggHome == null) {
                    throw (GridException)new GridException("Spring XML configuration file path is invalid: " + new File(springXmlPath) +
                        ". Note that this path should be either absolute path or a relative path to GRIDGAIN_HOME.").setData(85, "src/java/org/gridgain/grid/loaders/memory/GridMemoryLoader.java");
                }
            }

            path = new File(ggHome, path.getPath());

            if (path.exists() == false) {
                throw (GridException)new GridException("Spring XML configuration file path is invalid: " + new File(springXmlPath) +
                    ". Note that this path should be either absolute path or a relative path to GRIDGAIN_HOME.").setData(93, "src/java/org/gridgain/grid/loaders/memory/GridMemoryLoader.java");
            }
        }

        if (path.isFile() == false) {
            throw (GridException)new GridException("Provided file path is not a file: " + path).setData(99, "src/java/org/gridgain/grid/loaders/memory/GridMemoryLoader.java");
        }

        XmlBeanFactory factory = null;

        try {
            factory = new XmlBeanFactory(new FileSystemResource(path));
        }
        catch (BeansException e) {
            throw (GridException)new GridException("Failed to instantiate Spring bean factory: " + e.getMessage(), e).setData(108, "src/java/org/gridgain/grid/loaders/memory/GridMemoryLoader.java");
        }

        Map cfgMap = null;

        try {
            // Note: Spring is not generics-friendly.
            cfgMap = factory.getBeansOfType(GridConfiguration.class);
        }
        catch (BeansException e) {
            throw (GridException)new GridException("Failed to instantiate bean [type=" + GridConfiguration.class + ", error=" +
                e.getMessage() + ']', e).setData(118, "src/java/org/gridgain/grid/loaders/memory/GridMemoryLoader.java");
        }

        if (cfgMap == null) {
            throw (GridException)new GridException("Failed to find a single grid factory configuration in: " + path).setData(123, "src/java/org/gridgain/grid/loaders/memory/GridMemoryLoader.java");
        }

        if (cfgMap.size() == 0) {
            throw (GridException)new GridException("Can't find grid factory configuration in: " + path).setData(127, "src/java/org/gridgain/grid/loaders/memory/GridMemoryLoader.java");
        }

        if (cfgMap.size() > 1) {
            throw (GridException)new GridException("Too many grid factory configurations in: " + path).setData(131, "src/java/org/gridgain/grid/loaders/memory/GridMemoryLoader.java");
        }

        assert cfgMap.size() == 1 : "ASSERTION [line=134, file=src/java/org/gridgain/grid/loaders/memory/GridMemoryLoader.java]";

        GridConfiguration cfg = (GridConfiguration)cfgMap.values().iterator().next();

        assert cfg != null : "ASSERTION [line=138, file=src/java/org/gridgain/grid/loaders/memory/GridMemoryLoader.java]";

        try {
            GridFactory.start(cfg);
        }
        catch (GridException e) {
            throw (GridException)new GridException("Failed to start grid: " + e.getMessage(), e).setData(144, "src/java/org/gridgain/grid/loaders/memory/GridMemoryLoader.java");
        }
    }
}
