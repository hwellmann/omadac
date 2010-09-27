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

import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.spi.*;
import org.springframework.beans.*;
import org.springframework.beans.factory.xml.*;

/**
 * Helper class which helps to read deployer and tasks information from
 * <tt>Spring</tt> configuration file.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridUriDeploymentSpringDocument {
    /** Initialized springs beans factory. */
    private final XmlBeanFactory factory;

    /** List of tasks from GAR description. */
    private List<Class<? extends GridTask<?, ?>>> tasks = null;

    /**
     * Creates new instance of configuration helper with given configuration.
     *
     * @param factory Configuration factory.
     */
    GridUriDeploymentSpringDocument(XmlBeanFactory factory) {
        assert factory != null : "ASSERTION [line=51, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpringDocument.java]";

        this.factory = factory;
    }

    /**
     * Loads tasks declared in configuration by given class loader.
     *
     * @param clsldr Class loader.
     * @return Declared tasks.
     * @throws GridSpiException Thrown if there are no tasks in
     *      configuration or configuration could not be read.
     */
    @SuppressWarnings({"unchecked"})
    List<Class<? extends GridTask<?, ?>>> getTasks(ClassLoader clsldr) throws GridSpiException {
        assert clsldr!= null : "ASSERTION [line=66, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpringDocument.java]";

        try {
            if (tasks == null) {
                tasks = new ArrayList<Class<? extends GridTask<?, ?>>>();

                Map<String, List<String>> beans = factory.getBeansOfType(List.class);

                if (beans.size() > 0) {
                    for (List<String> list : beans.values()) {
                        for (String clsName : list) {
                            Class taskCls = null;

                            try {
                                taskCls = clsldr.loadClass(clsName);
                            }
                            catch (ClassNotFoundException e) {
                                throw (GridSpiException)new GridSpiException("Failed to load task class [className=" + clsName + ']',
                                    e).setData(83, "src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpringDocument.java");
                            }

                            assert taskCls != null : "ASSERTION [line=87, file=src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpringDocument.java]";

                            tasks.add(taskCls);
                        }
                    }
                }
            }
        }
        catch (BeansException e) {
            throw (GridSpiException)new GridSpiException("Failed to get tasks declared in XML file.", e).setData(96, "src/java/org/gridgain/grid/spi/deployment/uri/GridUriDeploymentSpringDocument.java");
        }

        return tasks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridUriDeploymentSpringDocument.class, this);
    }
}
